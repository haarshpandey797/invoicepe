package com.invoicepe.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CurrentSubscriptionResponse {

    private String plan;
    private String subscriptionId;
    private String status; // active / none

    public static CurrentSubscriptionResponse from(String plan, String subscriptionId) {
        boolean paid = plan != null && !"free".equals(plan);
        return CurrentSubscriptionResponse.builder()
                .plan(plan)
                .subscriptionId(subscriptionId)
                .status(paid ? "active" : "none")
                .build();
    }
}
