package com.invoicepe.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RazorpayServiceTest {

    @Test
    void hmacSha256MatchesKnownTestVector() {
        // RFC-style known vector: HMAC-SHA256(key="key", "The quick brown fox jumps over the lazy dog")
        String expected = "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8";
        assertEquals(expected, RazorpayService.hmacSha256Hex(
                "The quick brown fox jumps over the lazy dog", "key"));
    }

    @Test
    void verifyAcceptsCorrectSignature() {
        String secret = "whsec_test_secret";
        String payload = "{\"event\":\"payment_link.paid\",\"amount\":118000}";
        String signature = RazorpayService.hmacSha256Hex(payload, secret);

        assertTrue(RazorpayService.verify(payload, signature, secret));
    }

    @Test
    void verifyRejectsTamperedSignatureOrPayload() {
        String secret = "whsec_test_secret";
        String payload = "{\"event\":\"payment_link.paid\"}";
        String signature = RazorpayService.hmacSha256Hex(payload, secret);

        assertFalse(RazorpayService.verify(payload, "deadbeef", secret));
        assertFalse(RazorpayService.verify(payload + "tampered", signature, secret));
        assertFalse(RazorpayService.verify(payload, signature, "wrong_secret"));
    }

    @Test
    void verifyRejectsNullOrBlankInputs() {
        assertFalse(RazorpayService.verify(null, "sig", "secret"));
        assertFalse(RazorpayService.verify("payload", null, "secret"));
        assertFalse(RazorpayService.verify("payload", "sig", null));
        assertFalse(RazorpayService.verify("payload", "sig", ""));
    }
}
