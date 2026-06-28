package com.invoicepe.service;

import com.invoicepe.dto.response.MonthlyEarning;
import com.invoicepe.model.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardServiceTest {

    @Test
    void aggregatesPaymentsByMonthWithZeroFilledBuckets() {
        YearMonth start = YearMonth.of(2026, 1);
        List<Payment> payments = List.of(
                payment("100.00", LocalDateTime.of(2026, 1, 10, 12, 0)),
                payment("50.00", LocalDateTime.of(2026, 1, 20, 9, 0)),
                payment("200.00", LocalDateTime.of(2026, 3, 5, 9, 0))
        );

        List<MonthlyEarning> result = DashboardService.aggregateMonthly(payments, start, 6);

        assertEquals(6, result.size());
        assertEquals("Jan 2026", result.get(0).month());
        assertEquals(new BigDecimal("150.00"), result.get(0).total());
        assertEquals(new BigDecimal("0.00"), result.get(1).total());   // Feb
        assertEquals("Mar 2026", result.get(2).month());
        assertEquals(new BigDecimal("200.00"), result.get(2).total()); // Mar
        assertEquals(new BigDecimal("0.00"), result.get(5).total());   // Jun
    }

    @Test
    void ignoresPaymentsOutsideTheWindow() {
        YearMonth start = YearMonth.of(2026, 1);
        List<Payment> payments = List.of(
                payment("999.00", LocalDateTime.of(2025, 12, 31, 23, 0)), // before window
                payment("888.00", LocalDateTime.of(2026, 8, 1, 0, 0)),    // after 6-month window
                payment("75.00", LocalDateTime.of(2026, 2, 14, 10, 0))    // in window (Feb)
        );

        List<MonthlyEarning> result = DashboardService.aggregateMonthly(payments, start, 6);

        BigDecimal total = result.stream()
                .map(MonthlyEarning::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("75.00"), total);
        assertEquals(new BigDecimal("75.00"), result.get(1).total()); // Feb
    }

    @Test
    void skipsPaymentsWithNullPaidAtOrAmount() {
        YearMonth start = YearMonth.of(2026, 1);
        List<Payment> payments = List.of(
                payment("100.00", null),
                payment(null, LocalDateTime.of(2026, 1, 5, 9, 0)),
                payment("40.00", LocalDateTime.of(2026, 1, 6, 9, 0))
        );

        List<MonthlyEarning> result = DashboardService.aggregateMonthly(payments, start, 6);

        assertEquals(new BigDecimal("40.00"), result.get(0).total());
    }

    private Payment payment(String amount, LocalDateTime paidAt) {
        return Payment.builder()
                .amount(amount == null ? null : new BigDecimal(amount))
                .paidAt(paidAt)
                .status("captured")
                .build();
    }
}
