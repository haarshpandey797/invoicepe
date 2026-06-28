package com.invoicepe.dto.response;

import java.math.BigDecimal;

/**
 * One month's earnings for the dashboard chart.
 *
 * @param month formatted label, e.g. "Jan 2026"
 * @param total total amount paid in that month
 */
public record MonthlyEarning(String month, BigDecimal total) {
}
