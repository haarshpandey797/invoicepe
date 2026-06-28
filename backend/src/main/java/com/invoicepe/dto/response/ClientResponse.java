package com.invoicepe.dto.response;

import com.invoicepe.model.Client;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ClientResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String gstin;
    private String billingAddress;
    private LocalDateTime createdAt;

    public static ClientResponse from(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .gstin(client.getGstin())
                .billingAddress(client.getBillingAddress())
                .createdAt(client.getCreatedAt())
                .build();
    }
}
