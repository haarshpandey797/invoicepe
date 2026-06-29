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
 * Sends invoice and reminder emails via the Resend HTTP API
 * (POST https://api.resend.com/emails).
 *
 * <p>All sends are best-effort: failures are logged and never thrown, so the
 * caller's flow (e.g. marking an invoice 'sent') is never blocked by email.
 * When Resend is not configured (placeholder/test values) or the client has no
 * email address, the send is skipped and {@code false} is returned.
 */
@Service
@Slf4j
public class EmailService {

    @Value("${resend.api-url:https://api.resend.com}")
    private String apiUrl;

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    @Value("${resend.from-name:InvoicePe}")
    private String fromName;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Emails the invoice (payment link + PDF link) to the client.
     *
     * @return true if Resend accepted the email, false otherwise
     */
    public boolean sendInvoiceEmail(Invoice invoice, String pdfUrl) {
        String to = invoice.getClient().getEmail();
        if (!isReady(to)) {
            log.warn("Email not configured or client email missing; skipping invoice {}",
                    invoice.getInvoiceNumber());
            return false;
        }
        String subject = "Invoice " + invoice.getInvoiceNumber()
                + " from " + businessName(invoice.getUser());
        return send(to, subject, buildInvoiceHtml(invoice, pdfUrl), invoice.getInvoiceNumber());
    }

    /**
     * Emails a payment reminder to the client.
     *
     * @return true if Resend accepted the email, false otherwise
     */
    public boolean sendReminderEmail(Invoice invoice, String reminderType) {
        String to = invoice.getClient().getEmail();
        if (!isReady(to)) {
            log.warn("Email not configured or client email missing; skipping reminder for {}",
                    invoice.getInvoiceNumber());
            return false;
        }
        String subject = "Payment reminder: Invoice " + invoice.getInvoiceNumber();
        return send(to, subject, buildReminderHtml(invoice, reminderType), invoice.getInvoiceNumber());
    }

    // --- HTTP ----------------------------------------------------------------

    private boolean send(String to, String subject, String html, String invoiceNumber) {
        String endpoint = trimTrailingSlash(apiUrl) + "/emails";

        JSONObject payload = new JSONObject();
        payload.put("from", fromHeader());
        payload.put("to", to);
        payload.put("subject", subject);
        payload.put("html", html);

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 == 2) {
                log.info("Email sent for invoice {} to {}", invoiceNumber, to);
                return true;
            }
            log.error("Email send failed for invoice {} ({}): {}",
                    invoiceNumber, response.statusCode(), response.body());
            return false;
        } catch (Exception e) {
            log.error("Email send error for invoice {}", invoiceNumber, e);
            return false;
        }
    }

    /** Resend accepts either "email" or "Display Name <email>" in the from field. */
    private String fromHeader() {
        return notBlank(fromName) ? fromName + " <" + fromEmail + ">" : fromEmail;
    }

    // --- HTML templates ------------------------------------------------------

    static String buildInvoiceHtml(Invoice invoice, String pdfUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p>Hi ").append(esc(invoice.getClient().getName())).append(",</p>");
        sb.append("<p>Here is your invoice <strong>").append(esc(invoice.getInvoiceNumber()))
                .append("</strong> from ").append(esc(businessName(invoice.getUser())))
                .append(" for <strong>&#8377;").append(money(invoice.getTotalAmount())).append("</strong>.</p>");
        if (notBlank(invoice.getRazorpayPaymentLinkUrl())) {
            sb.append("<p><a href=\"").append(esc(invoice.getRazorpayPaymentLinkUrl()))
                    .append("\">Pay securely online</a></p>");
        }
        if (notBlank(pdfUrl)) {
            sb.append("<p><a href=\"").append(esc(pdfUrl)).append("\">Download the invoice PDF</a></p>");
        }
        sb.append("<p>Thank you for your business!</p>");
        return sb.toString();
    }

    static String buildReminderHtml(Invoice invoice, String reminderType) {
        String opening = switch (reminderType == null ? "" : reminderType) {
            case "pre_due" -> "This is a friendly reminder that your invoice is due soon.";
            case "on_due" -> "This is a reminder that your invoice is due today.";
            default -> "This is a reminder that your invoice payment is pending.";
        };

        StringBuilder sb = new StringBuilder();
        sb.append("<p>Hi ").append(esc(invoice.getClient().getName())).append(",</p>");
        sb.append("<p>").append(opening).append("</p>");
        sb.append("<p>Invoice <strong>").append(esc(invoice.getInvoiceNumber()))
                .append("</strong> for <strong>&#8377;").append(money(invoice.getTotalAmount()))
                .append("</strong> (due ").append(invoice.getDueDate()).append(").</p>");
        if (notBlank(invoice.getRazorpayPaymentLinkUrl())) {
            sb.append("<p><a href=\"").append(esc(invoice.getRazorpayPaymentLinkUrl()))
                    .append("\">Pay now</a></p>");
        }
        sb.append("<p>Thank you!</p>");
        return sb.toString();
    }

    // --- helpers -------------------------------------------------------------

    private boolean isReady(String to) {
        return notBlank(to) && isConfigured(apiKey) && isConfigured(fromEmail);
    }

    /** Guards against placeholder/test config so we never make a real call in dev/tests. */
    private boolean isConfigured(String value) {
        return value != null && !value.isBlank()
                && !value.equalsIgnoreCase("REPLACE_ME")
                && !value.equalsIgnoreCase("placeholder")
                && !value.startsWith("your-")
                && !value.startsWith("test-")
                && !value.startsWith("<");
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

    /** Minimal HTML escaping for values interpolated into the email body. */
    static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
