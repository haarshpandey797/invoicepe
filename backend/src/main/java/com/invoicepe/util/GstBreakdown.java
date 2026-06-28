package com.invoicepe.util;

import java.math.BigDecimal;

/**
 * Result of a GST calculation. All monetary values are scaled to 2 decimals.
 */
public record GstBreakdown(
        BigDecimal subtotal,
        BigDecimal cgstRate,
        BigDecimal sgstRate,
        BigDecimal igstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal total
) {
}
