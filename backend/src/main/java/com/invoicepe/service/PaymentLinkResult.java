package com.invoicepe.service;

/**
 * Result of a Razorpay payment-link creation.
 */
public record PaymentLinkResult(String id, String url) {
}
