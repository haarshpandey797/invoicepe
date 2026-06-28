package com.invoicepe.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class DashboardSummaryResponse {

    private BigDecimal totalEarnedThisMonth;
    private BigDecimal totalPending;
    private BigDecimal totalOverdue;
    private Map<String, Long> statusCounts;
}
