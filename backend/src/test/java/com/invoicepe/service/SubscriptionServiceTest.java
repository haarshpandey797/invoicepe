package com.invoicepe.service;

import com.invoicepe.exception.InvoicePeException;
import com.invoicepe.model.User;
import com.invoicepe.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private RazorpayService razorpayService;
    @Mock private UserRepository userRepository;
    @InjectMocks private SubscriptionService service;

    private static final String SUB_ID = "sub_123";

    @Test
    void rejectsWebhookWithInvalidSignature() {
        when(razorpayService.verifyWebhookSignature(any(), any())).thenReturn(false);

        InvoicePeException ex = assertThrows(InvoicePeException.class,
                () -> service.handleWebhook("{}", "bad-signature"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(userRepository, never()).findByRazorpaySubscriptionId(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void activatedEventUpgradesUserPlan() {
        User user = user("free");
        when(razorpayService.verifyWebhookSignature(any(), any())).thenReturn(true);
        when(userRepository.findByRazorpaySubscriptionId(SUB_ID)).thenReturn(Optional.of(user));
        when(razorpayService.planTypeForPlanId("plan_pro")).thenReturn("pro");

        service.handleWebhook(activatedPayload("plan_pro"), "sig");

        assertEquals("pro", user.getPlan());
        verify(userRepository).save(user);
    }

    @Test
    void cancelledEventDowngradesUserToFree() {
        User user = user("pro");
        when(razorpayService.verifyWebhookSignature(any(), any())).thenReturn(true);
        when(userRepository.findByRazorpaySubscriptionId(SUB_ID)).thenReturn(Optional.of(user));

        service.handleWebhook(cancelledPayload(), "sig");

        assertEquals("free", user.getPlan());
        verify(userRepository).save(user);
    }

    @Test
    void ignoresWebhookForUnknownSubscription() {
        when(razorpayService.verifyWebhookSignature(any(), any())).thenReturn(true);
        when(userRepository.findByRazorpaySubscriptionId(SUB_ID)).thenReturn(Optional.empty());

        service.handleWebhook(cancelledPayload(), "sig");

        verify(userRepository, never()).save(any());
    }

    private User user(String plan) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("u@test.com")
                .passwordHash("hash")
                .fullName("Test User")
                .plan(plan)
                .razorpaySubscriptionId(SUB_ID)
                .build();
    }

    private String activatedPayload(String planId) {
        return "{\"event\":\"subscription.activated\",\"payload\":{\"subscription\":{\"entity\":{"
                + "\"id\":\"" + SUB_ID + "\",\"plan_id\":\"" + planId + "\"}}}}";
    }

    private String cancelledPayload() {
        return "{\"event\":\"subscription.cancelled\",\"payload\":{\"subscription\":{\"entity\":{"
                + "\"id\":\"" + SUB_ID + "\"}}}}";
    }
}
