package com.invoicepe.controller;

import com.invoicepe.config.UserPrincipal;
import com.invoicepe.dto.request.ClientRequest;
import com.invoicepe.dto.response.ApiResponse;
import com.invoicepe.dto.response.ClientResponse;
import com.invoicepe.model.User;
import com.invoicepe.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientResponse>>> listClients(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ClientResponse> clients = clientService.listClients(principal.getUser());
        return ResponseEntity.ok(ApiResponse.success(clients));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ClientRequest request) {

        ClientResponse client = clientService.createClient(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Client created", client));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ClientRequest request) {

        ClientResponse client = clientService.updateClient(principal.getUser(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Client updated", client));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClient(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        clientService.deleteClient(principal.getUser(), id);
        return ResponseEntity.ok(ApiResponse.success("Client deleted", null));
    }
}
