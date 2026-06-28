package com.invoicepe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSubscriptionRequest {

    @NotBlank(message = "Plan type is required")
    private String planType;
}
