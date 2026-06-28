package com.invoicepe.service;

import com.invoicepe.dto.response.CurrentSubscriptionResponse;
import com.invoicepe.dto.response.SubscriptionResponse;
import com.invoicepe.exception.InvoicePeException;
import com.invoicepe.model.User;
import com.invoicepe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages InvoicePe's own Razorpay subscription billing (Pro / Business plans).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private static final String PLAN_FREE = "free";

    private final RazorpayService razorpayService;
    private final UserRepository userRepository;

    /**
     * Creates a Razorpay subscription for the user and stores its id so the
     * webhook can later resolve which user to upgrade/downgrade.
     */
    @Transactional
    public SubscriptionResponse create(User user, String planType) {
        String subscriptionId = razorpayService.createSubscription(planType);
        user.setRazorpaySubscriptionId(subscriptionId);
        userRepository.save(user);
        return SubscriptionResponse.builder()
                .subscriptionId(subscriptionId)
                .keyId(razorpayService.getKeyId())
                .planType(planType.toLowerCase())
                .build();
    }

    @Transactional(readOnly = true)
    public CurrentSubscriptionResponse current(User user) {
        return CurrentSubscriptionResponse.from(user.getPlan(), user.getRazorpaySubscriptionId());
    }

    /**
     * Handles Razorpay subscription webhooks. Activation upgrades the user's plan;
     * halt/cancel/complete downgrades back to free.
     */
    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!razorpayService.verifyWebhookSignature(payload, signature)) {
            log.warn("Rejected subscription webhook with invalid signature");
            throw new InvoicePeException("Invalid webhook signature", HttpStatus.UNAUTHORIZED);
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.optString("event", "");

        JSONObject entity = extractSubscriptionEntity(event);
        if (entity == null) {
            log.info("Subscription webhook {} had no subscription entity; ignoring", eventType);
            return;
        }

        String subscriptionId = entity.optString("id", null);
        if (subscriptionId == null) {
            return;
        }

        User user = userRepository.findByRazorpaySubscriptionId(subscriptionId).orElse(null);
        if (user == null) {
            log.warn("No user matched subscription {} from webhook {}", subscriptionId, eventType);
            return;
        }

        switch (eventType) {
            case "subscription.activated", "subscription.charged", "subscription.resumed" -> {
                String planType = razorpayService.planTypeForPlanId(entity.optString("plan_id", null));
                if (planType != null) {
                    user.setPlan(planType);
                    userRepository.save(user);
                    log.info("User {} upgraded to {} via subscription {}",
                            user.getId(), planType, subscriptionId);
                }
            }
            case "subscription.halted", "subscription.cancelled", "subscription.completed" -> {
                user.setPlan(PLAN_FREE);
                userRepository.save(user);
                log.info("User {} downgraded to free via subscription {} ({})",
                        user.getId(), subscriptionId, eventType);
            }
            default -> log.info("Ignoring subscription webhook event: {}", eventType);
        }
    }

    private JSONObject extractSubscriptionEntity(JSONObject event) {
        JSONObject payload = event.optJSONObject("payload");
        if (payload == null) {
            return null;
        }
        JSONObject subscription = payload.optJSONObject("subscription");
        if (subscription == null) {
            return null;
        }
        return subscription.optJSONObject("entity");
    }
}
