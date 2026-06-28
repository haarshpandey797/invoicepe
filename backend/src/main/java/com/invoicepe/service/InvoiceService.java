package com.invoicepe.service;

import com.invoicepe.dto.request.CreateInvoiceRequest;
import com.invoicepe.dto.request.InvoiceItemRequest;
import com.invoicepe.dto.response.InvoiceResponse;
import com.invoicepe.dto.response.PagedResponse;
import com.invoicepe.exception.InvoicePeException;
import com.invoicepe.exception.ResourceNotFoundException;
import com.invoicepe.model.Client;
import com.invoicepe.model.Invoice;
import com.invoicepe.model.InvoiceItem;
import com.invoicepe.model.User;
import com.invoicepe.repository.ClientRepository;
import com.invoicepe.repository.InvoiceRepository;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private static final String STATUS_DRAFT = "draft";

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final GstCalculator gstCalculator;
    private final InvoiceNumberGenerator invoiceNumberGenerator;
    private final PdfService pdfService;

    @Transactional
    public InvoiceResponse create(User user, CreateInvoiceRequest request) {
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
    public PagedResponse<InvoiceResponse> list(User user, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Invoice> result = (status == null || status.isBlank())
                ? invoiceRepository.findByUserId(user.getId(), pageable)
                : invoiceRepository.findByUserIdAndStatus(user.getId(), status, pageable);
        return PagedResponse.from(result.map(InvoiceResponse::from));
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
