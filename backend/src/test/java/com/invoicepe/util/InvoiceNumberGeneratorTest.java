package com.invoicepe.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvoiceNumberGeneratorTest {

    @Test
    void prefixUsesFirstThreeAlphanumericCharsUppercased() {
        assertEquals("RAH", InvoiceNumberGenerator.prefixFrom("Rahul Sharma"));
        assertEquals("ACM", InvoiceNumberGenerator.prefixFrom("Acme Studios"));
    }

    @Test
    void prefixSkipsNonAlphanumericCharacters() {
        assertEquals("ABT", InvoiceNumberGenerator.prefixFrom("&A. B-Tech"));
    }

    @Test
    void prefixPadsShortNames() {
        assertEquals("KXX", InvoiceNumberGenerator.prefixFrom("K"));
        assertEquals("ABX", InvoiceNumberGenerator.prefixFrom("AB"));
    }

    @Test
    void prefixFallsBackForBlankOrSymbolOnlyNames() {
        assertEquals("INV", InvoiceNumberGenerator.prefixFrom(""));
        assertEquals("INV", InvoiceNumberGenerator.prefixFrom("   "));
        assertEquals("INV", InvoiceNumberGenerator.prefixFrom("!!!"));
        assertEquals("INV", InvoiceNumberGenerator.prefixFrom(null));
    }

    @Test
    void formatZeroPadsSequenceToThreeDigits() {
        assertEquals("INV-2024-RSH-047", InvoiceNumberGenerator.format(2024, "RSH", 47));
        assertEquals("INV-2024-RSH-001", InvoiceNumberGenerator.format(2024, "RSH", 1));
        assertEquals("INV-2026-ACM-1234", InvoiceNumberGenerator.format(2026, "ACM", 1234));
    }
}
