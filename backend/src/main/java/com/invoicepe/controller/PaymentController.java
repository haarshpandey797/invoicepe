package com.invoicepe.controller;

import com.invoicepe.config.UserPrincipal;
import com.invoicepe.dto.response.ApiResponse;
import com.invoicepe.dto.response.PaymentResponse;
import com.invoicepe.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Razorpay webhook. No JWT auth (whitelisted); signature is verified internally.
     * Reads the raw body so the HMAC signature check matches exactly.
     */
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok(ApiResponse.success("Webhook processed", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> history(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(paymentService.history(principal.getUser())));
    }
}
