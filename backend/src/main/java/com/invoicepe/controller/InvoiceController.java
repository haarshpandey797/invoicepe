package com.invoicepe.controller;

import com.invoicepe.config.UserPrincipal;
import com.invoicepe.dto.request.CreateInvoiceRequest;
import com.invoicepe.dto.response.ApiResponse;
import com.invoicepe.dto.response.InvoiceResponse;
import com.invoicepe.dto.response.PagedResponse;
import com.invoicepe.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<InvoiceResponse> invoices =
                invoiceService.list(principal.getUser(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateInvoiceRequest request) {

        InvoiceResponse invoice = invoiceService.create(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created", invoice));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        return ResponseEntity.ok(ApiResponse.success(invoiceService.getById(principal.getUser(), id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody CreateInvoiceRequest request) {

        InvoiceResponse invoice = invoiceService.update(principal.getUser(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Invoice updated", invoice));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        invoiceService.delete(principal.getUser(), id);
        return ResponseEntity.ok(ApiResponse.success("Invoice deleted", null));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        byte[] pdf = invoiceService.generatePdf(principal.getUser(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"")
                .body(pdf);
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<InvoiceResponse>> send(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        InvoiceResponse invoice = invoiceService.send(principal.getUser(), id);
        return ResponseEntity.ok(ApiResponse.success("Invoice sent", invoice));
    }

    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markPaid(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {

        InvoiceResponse invoice = invoiceService.markPaid(principal.getUser(), id);
        return ResponseEntity.ok(ApiResponse.success("Invoice marked as paid", invoice));
    }
}
