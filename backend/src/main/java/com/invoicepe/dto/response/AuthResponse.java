package com.invoicepe.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuthResponse {

    private String token;
    private UUID userId;
    private String email;
    private String fullName;
    private String plan;
}
