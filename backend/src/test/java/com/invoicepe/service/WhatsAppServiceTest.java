package com.invoicepe.service;

import com.invoicepe.model.Client;
import com.invoicepe.model.Invoice;
import com.invoicepe.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppServiceTest {

    @Test
    void normalizePhoneAddsIndiaCodeToTenDigitNumbers() {
        assertEquals("919876543210", WhatsAppService.normalizePhone("9876543210"));
        assertEquals("919876543210", WhatsAppService.normalizePhone("+91 98765 43210"));
        assertEquals("919876543210", WhatsAppService.normalizePhone("919876543210"));
        assertNull(WhatsAppService.normalizePhone(null));
        assertNull(WhatsAppService.normalizePhone("   "));
    }

    @Test
    void invoiceMessageIncludesKeyDetails() {
        Invoice invoice = sampleInvoice();
        String message = WhatsAppService.buildInvoiceMessage(invoice);

        assertTrue(message.contains("Globex Pvt Ltd"), "should greet the client");
        assertTrue(message.contains("INV-2026-ACM-001"), "should include the invoice number");
        assertTrue(message.contains("1180.00"), "should include the total amount");
        assertTrue(message.contains("https://rzp.io/i/abc123"), "should include the payment link");
    }

    @Test
    void reminderMessageVariesByType() {
        Invoice invoice = sampleInvoice();

        assertTrue(WhatsAppService.buildReminderMessage(invoice, "pre_due").contains("due soon"));
        assertTrue(WhatsAppService.buildReminderMessage(invoice, "on_due").contains("due today"));
        assertTrue(WhatsAppService.buildReminderMessage(invoice, "post_due_7").contains("pending"));
        assertTrue(WhatsAppService.buildReminderMessage(invoice, "pre_due").contains("INV-2026-ACM-001"));
    }

    private Invoice sampleInvoice() {
        User user = User.builder().businessName("Acme Studios").fullName("Rahul Sharma").build();
        Client client = Client.builder().name("Globex Pvt Ltd").phone("9876543210").build();
        return Invoice.builder()
                .invoiceNumber("INV-2026-ACM-001")
                .user(user)
                .client(client)
                .dueDate(LocalDate.of(2026, 1, 15))
                .totalAmount(new BigDecimal("1180.00"))
                .razorpayPaymentLinkUrl("https://rzp.io/i/abc123")
                .build();
    }
}
