package com.invoicepe.controller;

import com.invoicepe.config.UserPrincipal;
import com.invoicepe.dto.response.ApiResponse;
import com.invoicepe.dto.response.DashboardSummaryResponse;
import com.invoicepe.dto.response.MonthlyEarning;
import com.invoicepe.dto.response.RecentInvoiceResponse;
import com.invoicepe.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(dashboardService.summary(principal.getUser())));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<RecentInvoiceResponse>>> recent(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(dashboardService.recent(principal.getUser())));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<MonthlyEarning>>> monthly(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(dashboardService.monthly(principal.getUser())));
    }
}
