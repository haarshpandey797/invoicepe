package com.invoicepe.service;

import com.invoicepe.model.Client;
import com.invoicepe.model.Invoice;
import com.invoicepe.model.InvoiceItem;
import com.invoicepe.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    @Test
    void generatesNonEmptyPdfWithPdfMagicHeader() {
        byte[] pdf = pdfService.generateInvoicePdf(sampleInvoice());

        assertNotNull(pdf);
        assertTrue(pdf.length > 0, "PDF should not be empty");
        String header = new String(pdf, 0, 4, StandardCharsets.US_ASCII);
        assertTrue(header.equals("%PDF"), "PDF should start with %PDF magic bytes, was: " + header);
    }

    @Test
    void generatesPdfForInterStateInvoice() {
        Invoice invoice = sampleInvoice();
        invoice.setCgstAmount(BigDecimal.ZERO);
        invoice.setSgstAmount(BigDecimal.ZERO);
        invoice.setIgstRate(new BigDecimal("18.00"));
        invoice.setIgstAmount(new BigDecimal("180.00"));

        byte[] pdf = pdfService.generateInvoicePdf(invoice);

        assertTrue(pdf.length > 0);
    }

    private Invoice sampleInvoice() {
        User user = User.builder()
                .businessName("Acme Studios")
                .fullName("Rahul Sharma")
                .gstin("27AAAAA0000A1Z5")
                .address("123 MG Road, Mumbai")
                .bankAccountName("Acme Studios")
                .bankAccountNumber("1234567890")
                .bankIfsc("HDFC0001234")
                .upiId("acme@upi")
                .build();

        Client client = Client.builder()
                .name("Globex Pvt Ltd")
                .billingAddress("42 Residency Road, Mumbai")
                .gstin("27BBBBB1111B2Z6")
                .build();

        InvoiceItem item = InvoiceItem.builder()
                .description("Website development")
                .hsnSacCode("998314")
                .quantity(new BigDecimal("1"))
                .unit("service")
                .rate(new BigDecimal("1000.00"))
                .amount(new BigDecimal("1000.00"))
                .displayOrder(0)
                .build();

        return Invoice.builder()
                .invoiceNumber("INV-2026-ACM-001")
                .status("draft")
                .user(user)
                .client(client)
                .issueDate(LocalDate.of(2026, 1, 1))
                .dueDate(LocalDate.of(2026, 1, 15))
                .items(new java.util.ArrayList<>(List.of(item)))
                .subtotal(new BigDecimal("1000.00"))
                .cgstRate(new BigDecimal("9.00"))
                .sgstRate(new BigDecimal("9.00"))
                .igstRate(new BigDecimal("0.00"))
                .cgstAmount(new BigDecimal("90.00"))
                .sgstAmount(new BigDecimal("90.00"))
                .igstAmount(new BigDecimal("0.00"))
                .totalAmount(new BigDecimal("1180.00"))
                .build();
    }
}
