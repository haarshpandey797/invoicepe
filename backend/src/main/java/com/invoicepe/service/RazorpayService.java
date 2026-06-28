package com.invoicepe.service;

import com.invoicepe.exception.InvoicePeException;
import com.invoicepe.model.Invoice;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Razorpay integration: payment-link creation and webhook signature verification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    /**
     * Creates a Razorpay payment link for the invoice total.
     */
    public PaymentLinkResult createPaymentLink(Invoice invoice) {
        try {
            JSONObject request = new JSONObject();
            request.put("amount", toPaise(invoice.getTotalAmount()));
            request.put("currency", "INR");
            request.put("accept_partial", false);
            request.put("description", "Payment for invoice " + invoice.getInvoiceNumber());
            request.put("reference_id", invoice.getId().toString());

            JSONObject customer = new JSONObject();
            customer.put("name", invoice.getClient().getName());
            if (notBlank(invoice.getClient().getEmail())) {
                customer.put("email", invoice.getClient().getEmail());
            }
            if (notBlank(invoice.getClient().getPhone())) {
                customer.put("contact", invoice.getClient().getPhone());
            }
            request.put("customer", customer);

            JSONObject notify = new JSONObject();
            notify.put("sms", true);
            notify.put("email", true);
            request.put("notify", notify);

            PaymentLink link = razorpayClient.paymentLink.create(request);
            String id = link.get("id");
            String shortUrl = link.get("short_url");
            log.info("Created Razorpay payment link {} for invoice {}", id, invoice.getInvoiceNumber());
            return new PaymentLinkResult(id, shortUrl);
        } catch (RazorpayException e) {
            log.error("Razorpay payment link creation failed for invoice {}", invoice.getInvoiceNumber(), e);
            throw new InvoicePeException("Failed to create payment link: " + e.getMessage());
        }
    }

    /**
     * Verifies the X-Razorpay-Signature header (HMAC-SHA256 hex of the raw body).
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        return verify(payload, signature, webhookSecret);
    }

    static boolean verify(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null || secret.isBlank()) {
            return false;
        }
        String expected = hmacSha256Hex(payload, secret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return new String(Hex.encode(raw));
        } catch (Exception e) {
            throw new InvoicePeException("Unable to compute webhook signature");
        }
    }

    private long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
