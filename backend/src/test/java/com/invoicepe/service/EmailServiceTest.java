package com.invoicepe.service;

import com.invoicepe.model.Client;
import com.invoicepe.model.Invoice;
import com.invoicepe.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailServiceTest {

    private Invoice invoice(String clientName) {
        User user = User.builder().businessName("Acme Studio").fullName("Rahul").build();
        Client client = Client.builder().name(clientName).email("client@test.com").build();
        return Invoice.builder()
                .invoiceNumber("INV-2026-ACM-001")
                .user(user)
                .client(client)
                .totalAmount(new BigDecimal("1180.00"))
                .dueDate(LocalDate.of(2026, 7, 15))
                .razorpayPaymentLinkUrl("https://rzp.io/i/abc123")
                .build();
    }

    @Test
    void invoiceHtmlContainsNumberBusinessAmountAndLinks() {
        String html = EmailService.buildInvoiceHtml(invoice("Priya"), "https://files/pdf");

        assertTrue(html.contains("INV-2026-ACM-001"));
        assertTrue(html.contains("Acme Studio"));
        assertTrue(html.contains("1180.00"));
        assertTrue(html.contains("https://rzp.io/i/abc123"));
        assertTrue(html.contains("https://files/pdf"));
    }

    @Test
    void invoiceHtmlOmitsPdfLinkWhenNull() {
        String html = EmailService.buildInvoiceHtml(invoice("Priya"), null);

        assertTrue(html.contains("https://rzp.io/i/abc123"));
        assertFalse(html.contains("Download the invoice PDF"));
    }

    @Test
    void reminderHtmlVariesByType() {
        assertTrue(EmailService.buildReminderHtml(invoice("Priya"), "pre_due").contains("due soon"));
        assertTrue(EmailService.buildReminderHtml(invoice("Priya"), "on_due").contains("due today"));
        assertTrue(EmailService.buildReminderHtml(invoice("Priya"), "post_due_3").contains("pending"));
    }

    @Test
    void escapesHtmlSpecialCharsInClientName() {
        String html = EmailService.buildInvoiceHtml(invoice("<script>&\"x"), null);

        assertTrue(html.contains("&lt;script&gt;&amp;&quot;x"));
        assertFalse(html.contains("<script>"));
    }

    @Test
    void escHandlesNull() {
        assertEquals("", EmailService.esc(null));
    }
}
