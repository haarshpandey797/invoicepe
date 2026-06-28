package com.invoicepe.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionResponse {

    private String subscriptionId;
    private String keyId;
    private String planType;
}
