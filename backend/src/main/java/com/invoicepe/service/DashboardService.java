package com.invoicepe.service;

import com.invoicepe.dto.response.DashboardSummaryResponse;
import com.invoicepe.dto.response.MonthlyEarning;
import com.invoicepe.dto.response.RecentInvoiceResponse;
import com.invoicepe.model.Payment;
import com.invoicepe.model.User;
import com.invoicepe.repository.InvoiceRepository;
import com.invoicepe.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int MONTHS_WINDOW = 6;
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(User user) {
        YearMonth thisMonth = YearMonth.from(LocalDate.now(IST));
        LocalDateTime monthStart = thisMonth.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart = thisMonth.plusMonths(1).atDay(1).atStartOfDay();

        BigDecimal earned = paymentRepository
                .sumAmountByUserIdAndPaidAtBetween(user.getId(), monthStart, nextMonthStart);
        BigDecimal pending = invoiceRepository.sumTotalAmountByUserIdAndStatus(user.getId(), "sent");
        BigDecimal overdue = invoiceRepository.sumTotalAmountByUserIdAndStatus(user.getId(), "overdue");

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (Object[] row : invoiceRepository.countByStatusForUser(user.getId())) {
            statusCounts.put((String) row[0], (Long) row[1]);
        }

        return DashboardSummaryResponse.builder()
                .totalEarnedThisMonth(earned)
                .totalPending(pending)
                .totalOverdue(overdue)
                .statusCounts(statusCounts)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecentInvoiceResponse> recent(User user) {
        return invoiceRepository.findTop5ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(RecentInvoiceResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MonthlyEarning> monthly(User user) {
        YearMonth start = YearMonth.from(LocalDate.now(IST)).minusMonths(MONTHS_WINDOW - 1L);
        LocalDateTime from = start.atDay(1).atStartOfDay();
        List<Payment> payments = paymentRepository.findByUserIdAndPaidAtGreaterThanEqual(user.getId(), from);
        return aggregateMonthly(payments, start, MONTHS_WINDOW);
    }

    /**
     * Buckets payment amounts into {@code months} consecutive months starting at
     * {@code start}, zero-filling empty months and ignoring payments outside the window.
     */
    static List<MonthlyEarning> aggregateMonthly(List<Payment> payments, YearMonth start, int months) {
        Map<YearMonth, BigDecimal> buckets = new LinkedHashMap<>();
        for (int i = 0; i < months; i++) {
            buckets.put(start.plusMonths(i), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        for (Payment payment : payments) {
            if (payment.getPaidAt() == null || payment.getAmount() == null) {
                continue;
            }
            YearMonth month = YearMonth.from(payment.getPaidAt());
            BigDecimal current = buckets.get(month);
            if (current != null) {
                buckets.put(month, current.add(payment.getAmount()).setScale(2, RoundingMode.HALF_UP));
            }
        }

        return buckets.entrySet().stream()
                .map(entry -> new MonthlyEarning(entry.getKey().format(MONTH_LABEL), entry.getValue()))
                .toList();
    }
}
