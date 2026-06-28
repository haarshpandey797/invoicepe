package com.invoicepe.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GstCalculatorTest {

    private final GstCalculator calculator = new GstCalculator();

    // GSTINs: first 2 digits are the state code. 27 = Maharashtra, 29 = Karnataka.
    private static final String MH_GSTIN = "27AAAAA0000A1Z5";
    private static final String MH_GSTIN_2 = "27BBBBB1111B2Z6";
    private static final String KA_GSTIN = "29CCCCC2222C3Z7";

    @Test
    void sameStateAppliesCgstAndSgst() {
        GstBreakdown result = calculator.calculate(new BigDecimal("1000.00"), MH_GSTIN, MH_GSTIN_2);

        assertEquals(new BigDecimal("9.00"), result.cgstRate());
        assertEquals(new BigDecimal("9.00"), result.sgstRate());
        assertEquals(new BigDecimal("0.00"), result.igstRate());
        assertEquals(new BigDecimal("90.00"), result.cgstAmount());
        assertEquals(new BigDecimal("90.00"), result.sgstAmount());
        assertEquals(new BigDecimal("0.00"), result.igstAmount());
        assertEquals(new BigDecimal("1180.00"), result.total());
    }

    @Test
    void interStateAppliesIgst() {
        GstBreakdown result = calculator.calculate(new BigDecimal("1000.00"), MH_GSTIN, KA_GSTIN);

        assertEquals(new BigDecimal("0.00"), result.cgstRate());
        assertEquals(new BigDecimal("0.00"), result.sgstRate());
        assertEquals(new BigDecimal("18.00"), result.igstRate());
        assertEquals(new BigDecimal("0.00"), result.cgstAmount());
        assertEquals(new BigDecimal("0.00"), result.sgstAmount());
        assertEquals(new BigDecimal("180.00"), result.igstAmount());
        assertEquals(new BigDecimal("1180.00"), result.total());
    }

    @Test
    void missingClientGstinDefaultsToIntraState() {
        GstBreakdown result = calculator.calculate(new BigDecimal("500.00"), MH_GSTIN, null);

        assertEquals(new BigDecimal("45.00"), result.cgstAmount());
        assertEquals(new BigDecimal("45.00"), result.sgstAmount());
        assertEquals(new BigDecimal("0.00"), result.igstAmount());
        assertEquals(new BigDecimal("590.00"), result.total());
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        // 333.33 * 9% = 29.9997 -> 30.00
        GstBreakdown result = calculator.calculate(new BigDecimal("333.33"), MH_GSTIN, MH_GSTIN_2);

        assertEquals(new BigDecimal("30.00"), result.cgstAmount());
        assertEquals(new BigDecimal("30.00"), result.sgstAmount());
        assertEquals(new BigDecimal("393.33"), result.total());
    }

    @Test
    void isInterStateLogic() {
        assertTrue(calculator.isInterState(MH_GSTIN, KA_GSTIN));
        assertFalse(calculator.isInterState(MH_GSTIN, MH_GSTIN_2));
        assertFalse(calculator.isInterState(null, KA_GSTIN));
        assertFalse(calculator.isInterState(MH_GSTIN, null));
    }
}
