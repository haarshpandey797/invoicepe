package com.invoicepe.service;

import com.invoicepe.dto.request.CreateInvoiceRequest;
import com.invoicepe.dto.request.InvoiceItemRequest;
import com.invoicepe.dto.response.InvoiceResponse;
import com.invoicepe.dto.response.PagedResponse;
import com.invoicepe.dto.response.ReminderLogResponse;
import com.invoicepe.exception.InvoicePeException;
import com.invoicepe.exception.PlanLimitException;
import com.invoicepe.exception.ResourceNotFoundException;
import com.invoicepe.model.Client;
import com.invoicepe.model.Invoice;
import com.invoicepe.model.InvoiceItem;
import com.invoicepe.model.Payment;
import com.invoicepe.model.User;
import com.invoicepe.repository.ClientRepository;
import com.invoicepe.repository.InvoiceRepository;
import com.invoicepe.repository.PaymentRepository;
import com.invoicepe.repository.ReminderLogRepository;
import com.invoicepe.util.GstBreakdown;
import com.invoicepe.util.GstCalculator;
import com.invoicepe.util.InvoiceNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_SENT = "sent";
    private static final String STATUS_PAID = "paid";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String PLAN_FREE = "free";
    private static final int FREE_PLAN_MONTHLY_LIMIT = 5;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final PaymentRepository paymentRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final GstCalculator gstCalculator;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final PdfService pdfService;
    private final StorageService storageService;
    private final RazorpayService razorpayService;
    private final WhatsAppService whatsAppService;

    @Transactional
    public InvoiceResponse create(User user, CreateInvoiceRequest request) {
        if (PLAN_FREE.equals(user.getPlan())) {
            LocalDate now = LocalDate.now(IST);
            long monthCount = invoiceRepository.countByUserIdAndYearAndMonth(
                    user.getId(), now.getYear(), now.getMonthValue());
            if (monthCount >= FREE_PLAN_MONTHLY_LIMIT) {
                throw new PlanLimitException(
                        "Free plan allows only " + FREE_PLAN_MONTHLY_LIMIT +
                        " invoices per month. Upgrade to Pro for unlimited invoices.");
            }
        }

        Client client = requireClient(user, request.getClientId());
        validateDates(request);

        Invoice invoice = Invoice.builder()
                .user(user)
                .client(client)
                .invoiceNumber(invoiceNumberGenerator.generate(user))
                .status(STATUS_DRAFT)
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate())
                .notes(request.getNotes())
                .terms(request.getTerms())
                .build();

        applyItemsAndTotals(invoice, request.getItems(), user, client);

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice {} created for user {}", invoice.getInvoiceNumber(), user.getId());
        return InvoiceResponse.from(invoice);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InvoiceResponse> list(User user, String status, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String statusFilter = (status == null || status.isBlank()) ? null : status;
        String searchFilter = (search == null || search.isBlank()) ? null : search.trim();
        Page<Invoice> result =
                invoiceRepository.searchForUser(user.getId(), statusFilter, searchFilter, pageable);
        return PagedResponse.from(result.map(InvoiceResponse::from));
    }

    @Transactional(readOnly = true)
    public List<ReminderLogResponse> getReminders(User user, UUID id) {
        Invoice invoice = requireInvoice(user, id); // enforces ownership
        return reminderLogRepository.findByInvoiceIdOrderBySentAtDesc(invoice.getId())
                .stream()
                .map(ReminderLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getById(User user, UUID id) {
        return InvoiceResponse.from(requireInvoice(user, id));
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(User user, UUID id) {
        Invoice invoice = requireInvoice(user, id);
        // Touch lazy associations so the PDF can be built inside this transaction.
        invoice.getItems().size();
        invoice.getClient().getName();
        return pdfService.generateInvoicePdf(invoice);
    }

    /**
     * Sends an invoice: generates the PDF, uploads it, creates a Razorpay payment
     * link, and marks the invoice as 'sent'. (WhatsApp delivery is wired separately.)
     */
    @Transactional
    public InvoiceResponse send(User user, UUID id) {
        if (PLAN_FREE.equals(user.getPlan())) {
            throw new InvoicePeException(
                    "Upgrade to Pro to send invoices with payment links and WhatsApp reminders",
                    HttpStatus.FORBIDDEN);
        }

        Invoice invoice = requireInvoice(user, id);
        if (STATUS_PAID.equals(invoice.getStatus()) || STATUS_CANCELLED.equals(invoice.getStatus())) {
            throw new InvoicePeException(
                    "Cannot send a " + invoice.getStatus() + " invoice", HttpStatus.CONFLICT);
        }
        invoice.getItems().size();
        invoice.getClient().getName();

        byte[] pdf = pdfService.generateInvoicePdf(invoice);
        String pdfUrl = storageService.uploadInvoicePdf(invoice.getInvoiceNumber(), pdf);
        if (pdfUrl != null) {
            invoice.setPdfUrl(pdfUrl);
        }

        PaymentLinkResult link = razorpayService.createPaymentLink(invoice);
        invoice.setRazorpayPaymentLinkId(link.id());
        invoice.setRazorpayPaymentLinkUrl(link.url());
        invoice.setStatus(STATUS_SENT);

        invoice = invoiceRepository.save(invoice);

        // Best-effort WhatsApp delivery; a failure must not undo the 'sent' status.
        try {
            whatsAppService.sendInvoiceMessage(invoice, invoice.getPdfUrl());
        } catch (Exception e) {
            log.error("WhatsApp delivery failed for invoice {}; invoice remains sent",
                    invoice.getInvoiceNumber(), e);
        }

        log.info("Invoice {} sent for user {}", invoice.getInvoiceNumber(), user.getId());
        return InvoiceResponse.from(invoice);
    }

    /**
     * Manually marks an invoice as paid (without Razorpay) and records the payment.
     */
    @Transactional
    public InvoiceResponse markPaid(User user, UUID id) {
        Invoice invoice = requireInvoice(user, id);
        if (STATUS_PAID.equals(invoice.getStatus())) {
            throw new InvoicePeException("Invoice is already paid", HttpStatus.CONFLICT);
        }

        invoice.setStatus(STATUS_PAID);
        invoiceRepository.save(invoice);

        Payment payment = Payment.builder()
                .invoice(invoice)
                .user(user)
                .amount(invoice.getTotalAmount())
                .currency("INR")
                .status("captured")
                .paymentMethod("manual")
                .paidAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        log.info("Invoice {} manually marked paid by user {}", invoice.getInvoiceNumber(), user.getId());
        return InvoiceResponse.from(invoice);
    }

    @Transactional
    public InvoiceResponse update(User user, UUID id, CreateInvoiceRequest request) {
        Invoice invoice = requireInvoice(user, id);
        requireDraft(invoice, "edited");

        Client client = requireClient(user, request.getClientId());
        validateDates(request);

        invoice.setClient(client);
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setNotes(request.getNotes());
        invoice.setTerms(request.getTerms());
        applyItemsAndTotals(invoice, request.getItems(), user, client);

        return InvoiceResponse.from(invoiceRepository.save(invoice));
    }

    @Transactional
    public void delete(User user, UUID id) {
        Invoice invoice = requireInvoice(user, id);
        requireDraft(invoice, "deleted");
        invoiceRepository.delete(invoice);
        log.info("Invoice {} deleted by user {}", invoice.getInvoiceNumber(), user.getId());
    }

    // --- helpers -------------------------------------------------------------

    private void applyItemsAndTotals(Invoice invoice, List<InvoiceItemRequest> itemRequests,
                                     User user, Client client) {
        invoice.getItems().clear();
        BigDecimal subtotal = BigDecimal.ZERO;
        int order = 0;

        for (InvoiceItemRequest req : itemRequests) {
            BigDecimal amount = req.getQuantity().multiply(req.getRate())
                    .setScale(2, RoundingMode.HALF_UP);

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .description(req.getDescription())
                    .hsnSacCode(req.getHsnSacCode())
                    .quantity(req.getQuantity())
                    .unit(req.getUnit() != null && !req.getUnit().isBlank() ? req.getUnit() : "service")
                    .rate(req.getRate())
                    .amount(amount)
                    .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : order)
                    .build();

            invoice.getItems().add(item);
            subtotal = subtotal.add(amount);
            order++;
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        GstBreakdown gst = gstCalculator.calculate(subtotal, user.getGstin(), client.getGstin());

        invoice.setSubtotal(gst.subtotal());
        invoice.setCgstRate(gst.cgstRate());
        invoice.setSgstRate(gst.sgstRate());
        invoice.setIgstRate(gst.igstRate());
        invoice.setCgstAmount(gst.cgstAmount());
        invoice.setSgstAmount(gst.sgstAmount());
        invoice.setIgstAmount(gst.igstAmount());
        invoice.setTotalAmount(gst.total());
    }

    private Client requireClient(User user, UUID clientId) {
        return clientRepository.findByIdAndUserId(clientId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client", String.valueOf(clientId)));
    }

    private Invoice requireInvoice(User user, UUID id) {
        return invoiceRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", String.valueOf(id)));
    }

    private void requireDraft(Invoice invoice, String action) {
        if (!STATUS_DRAFT.equals(invoice.getStatus())) {
            throw new InvoicePeException(
                    "Only draft invoices can be " + action, HttpStatus.CONFLICT);
        }
    }

    private void validateDates(CreateInvoiceRequest request) {
        if (request.getDueDate().isBefore(request.getIssueDate())) {
            throw new InvoicePeException("Due date cannot be before the issue date");
        }
    }
}
