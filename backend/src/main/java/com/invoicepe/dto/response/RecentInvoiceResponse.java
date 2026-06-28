package com.invoicepe.dto.response;

import com.invoicepe.model.Invoice;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class RecentInvoiceResponse {

    private UUID id;
    private String invoiceNumber;
    private String clientName;
    private String status;
    private BigDecimal totalAmount;
    private LocalDate dueDate;
    private LocalDateTime createdAt;

    public static RecentInvoiceResponse from(Invoice invoice) {
        return RecentInvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .clientName(invoice.getClient() != null ? invoice.getClient().getName() : null)
                .status(invoice.getStatus())
                .totalAmount(invoice.getTotalAmount())
                .dueDate(invoice.getDueDate())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
