package com.invoicepe.controller;

import com.invoicepe.config.UserPrincipal;
import com.invoicepe.dto.request.CreateSubscriptionRequest;
import com.invoicepe.dto.response.ApiResponse;
import com.invoicepe.dto.response.CurrentSubscriptionResponse;
import com.invoicepe.dto.response.SubscriptionResponse;
import com.invoicepe.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateSubscriptionRequest request) {

        SubscriptionResponse response =
                subscriptionService.create(principal.getUser(), request.getPlanType());
        return ResponseEntity.ok(ApiResponse.success("Subscription created", response));
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<CurrentSubscriptionResponse>> current(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(subscriptionService.current(principal.getUser())));
    }

    /**
     * Razorpay subscription webhook. No JWT auth (whitelisted); signature verified internally.
     */
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        subscriptionService.handleWebhook(payload, signature);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed", null));
    }
}
