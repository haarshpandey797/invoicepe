package com.invoicepe.service;

import com.invoicepe.dto.response.PaymentResponse;
import com.invoicepe.exception.InvoicePeException;
import com.invoicepe.model.Invoice;
import com.invoicepe.model.Payment;
import com.invoicepe.model.User;
import com.invoicepe.repository.InvoiceRepository;
import com.invoicepe.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Handles Razorpay webhooks and exposes payment history.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String STATUS_PAID = "paid";

    private final RazorpayService razorpayService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!razorpayService.verifyWebhookSignature(payload, signature)) {
            log.warn("Rejected Razorpay webhook with invalid signature");
            throw new InvoicePeException("Invalid webhook signature", HttpStatus.UNAUTHORIZED);
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.optString("event", "");
        if (!"payment_link.paid".equals(eventType)) {
            log.info("Ignoring Razorpay webhook event: {}", eventType);
            return;
        }

        JSONObject entities = event.getJSONObject("payload");
        JSONObject paymentLink = entities.getJSONObject("payment_link").getJSONObject("entity");
        JSONObject payment = entities.has("payment")
                ? entities.getJSONObject("payment").getJSONObject("entity")
                : null;

        Invoice invoice = resolveInvoice(paymentLink);
        if (invoice == null) {
            log.warn("No invoice matched webhook payment_link {}", paymentLink.optString("id"));
            return;
        }
        if (STATUS_PAID.equals(invoice.getStatus())) {
            log.info("Invoice {} already paid; ignoring duplicate webhook", invoice.getInvoiceNumber());
            return;
        }

        invoice.setStatus(STATUS_PAID);
        invoiceRepository.save(invoice);
        recordPayment(invoice, payment);
        log.info("Invoice {} marked paid via Razorpay webhook", invoice.getInvoiceNumber());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> history(User user) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    private Invoice resolveInvoice(JSONObject paymentLink) {
        String referenceId = paymentLink.optString("reference_id", null);
        if (referenceId != null && !referenceId.isBlank()) {
            try {
                Invoice byReference = invoiceRepository.findById(UUID.fromString(referenceId)).orElse(null);
                if (byReference != null) {
                    return byReference;
                }
            } catch (IllegalArgumentException ignored) {
                // reference_id is not a UUID; fall through to link-id lookup
            }
        }
        String linkId = paymentLink.optString("id", null);
        return linkId == null ? null
                : invoiceRepository.findByRazorpayPaymentLinkId(linkId).orElse(null);
    }

    private void recordPayment(Invoice invoice, JSONObject payment) {
        if (payment == null) {
            return;
        }
        String razorpayPaymentId = payment.optString("id", null);
        if (razorpayPaymentId != null
                && paymentRepository.findByRazorpayPaymentId(razorpayPaymentId).isPresent()) {
            return;
        }

        BigDecimal amount = BigDecimal.valueOf(payment.optLong("amount", 0L)).movePointLeft(2);
        Payment record = Payment.builder()
                .invoice(invoice)
                .user(invoice.getUser())
                .razorpayPaymentId(razorpayPaymentId)
                .razorpayOrderId(payment.optString("order_id", null))
                .amount(amount)
                .currency(payment.optString("currency", "INR"))
                .status("captured")
                .paymentMethod(payment.optString("method", null))
                .paidAt(LocalDateTime.now())
                .build();
        paymentRepository.save(record);
    }
}
