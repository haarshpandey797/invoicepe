package com.invoicepe.service;

import com.invoicepe.model.Invoice;
import com.invoicepe.model.User;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Sends invoice and reminder messages over the Meta WhatsApp Cloud API.
 * All sends are best-effort: failures are logged and never thrown, so the
 * caller's flow (e.g. marking an invoice 'sent') is never blocked by WhatsApp.
 */
@Service
@Slf4j
public class WhatsAppService {

    @Value("${whatsapp.api-url}")
    private String apiUrl;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.access-token}")
    private String accessToken;

    /**
     * Sends the invoice (PDF document if available, else text) with the payment link.
     *
     * @return true if WhatsApp accepted the message, false otherwise
     */
    public boolean sendInvoiceMessage(Invoice invoice, String pdfUrl) {
        String phone = normalizePhone(invoice.getClient().getPhone());
        if (!isReady(phone)) {
            log.warn("WhatsApp not configured or client phone missing; skipping invoice {}",
                    invoice.getInvoiceNumber());
            return false;
        }

        String message = buildInvoiceMessage(invoice);
        JSONObject payload = (pdfUrl != null && !pdfUrl.isBlank())
                ? documentPayload(phone, pdfUrl,
                        "invoice-" + invoice.getInvoiceNumber() + ".pdf", message)
                : textPayload(phone, message);

        return send(payload, invoice.getInvoiceNumber());
    }

    /**
     * Sends a payment reminder for the invoice.
     */
    public boolean sendReminderMessage(Invoice invoice, String reminderType) {
        String phone = normalizePhone(invoice.getClient().getPhone());
        if (!isReady(phone)) {
            log.warn("WhatsApp not configured or client phone missing; skipping reminder for {}",
                    invoice.getInvoiceNumber());
            return false;
        }
        return send(textPayload(phone, buildReminderMessage(invoice, reminderType)),
                invoice.getInvoiceNumber());
    }

    // --- HTTP ----------------------------------------------------------------

    private boolean send(JSONObject payload, String invoiceNumber) {
        String endpoint = trimTrailingSlash(apiUrl) + "/" + phoneNumberId + "/messages";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 == 2) {
                log.info("WhatsApp message sent for invoice {}", invoiceNumber);
                return true;
            }
            log.error("WhatsApp send failed for invoice {} ({}): {}",
                    invoiceNumber, response.statusCode(), response.body());
            return false;
        } catch (Exception e) {
            log.error("WhatsApp send error for invoice {}", invoiceNumber, e);
            return false;
        }
    }

    private JSONObject textPayload(String phone, String body) {
        JSONObject text = new JSONObject();
        text.put("preview_url", true);
        text.put("body", body);

        JSONObject payload = basePayload(phone);
        payload.put("type", "text");
        payload.put("text", text);
        return payload;
    }

    private JSONObject documentPayload(String phone, String link, String filename, String caption) {
        JSONObject document = new JSONObject();
        document.put("link", link);
        document.put("filename", filename);
        document.put("caption", caption);

        JSONObject payload = basePayload(phone);
        payload.put("type", "document");
        payload.put("document", document);
        return payload;
    }

    private JSONObject basePayload(String phone) {
        JSONObject payload = new JSONObject();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", phone);
        return payload;
    }

    // --- message templates ---------------------------------------------------

    static String buildInvoiceMessage(Invoice invoice) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(invoice.getClient().getName()).append(",\n\n");
        sb.append("Here is your invoice *").append(invoice.getInvoiceNumber())
                .append("* from ").append(businessName(invoice.getUser()))
                .append(" for *Rs. ").append(money(invoice.getTotalAmount())).append("*.\n\n");
        if (notBlank(invoice.getRazorpayPaymentLinkUrl())) {
            sb.append("You can pay securely here:\n")
                    .append(invoice.getRazorpayPaymentLinkUrl()).append("\n\n");
        }
        sb.append("Thank you for your business!");
        return sb.toString();
    }

    static String buildReminderMessage(Invoice invoice, String reminderType) {
        String opening = switch (reminderType == null ? "" : reminderType) {
            case "pre_due" -> "This is a friendly reminder that your invoice is due soon.";
            case "on_due" -> "This is a reminder that your invoice is due today.";
            default -> "This is a reminder that your invoice payment is pending.";
        };

        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(invoice.getClient().getName()).append(",\n\n");
        sb.append(opening).append("\n\n");
        sb.append("Invoice *").append(invoice.getInvoiceNumber())
                .append("* for *Rs. ").append(money(invoice.getTotalAmount()))
                .append("* (due ").append(invoice.getDueDate()).append(").\n\n");
        if (notBlank(invoice.getRazorpayPaymentLinkUrl())) {
            sb.append("Pay now:\n").append(invoice.getRazorpayPaymentLinkUrl()).append("\n\n");
        }
        sb.append("Thank you!");
        return sb.toString();
    }

    // --- helpers -------------------------------------------------------------

    static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "91" + digits;
        }
        return digits.isEmpty() ? null : digits;
    }

    private boolean isReady(String phone) {
        return phone != null && isConfigured(accessToken) && isConfigured(phoneNumberId);
    }

    private boolean isConfigured(String value) {
        return value != null && !value.isBlank()
                && !value.equalsIgnoreCase("REPLACE_ME")
                && !value.equalsIgnoreCase("placeholder")
                && !value.startsWith("your-");
    }

    private static String businessName(User user) {
        if (notBlank(user.getBusinessName())) {
            return user.getBusinessName();
        }
        return notBlank(user.getFullName()) ? user.getFullName() : "InvoicePe";
    }

    private static String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
