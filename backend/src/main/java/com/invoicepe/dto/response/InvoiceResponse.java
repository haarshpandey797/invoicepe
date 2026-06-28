package com.invoicepe.dto.response;

import com.invoicepe.model.Invoice;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class InvoiceResponse {

    private UUID id;
    private String invoiceNumber;
    private String status;
    private UUID clientId;
    private String clientName;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal cgstRate;
    private BigDecimal sgstRate;
    private BigDecimal igstRate;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal totalAmount;
    private String notes;
    private String terms;
    private String pdfUrl;
    private String razorpayPaymentLinkUrl;
    private List<InvoiceItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InvoiceResponse from(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .status(invoice.getStatus())
                .clientId(invoice.getClient() != null ? invoice.getClient().getId() : null)
                .clientName(invoice.getClient() != null ? invoice.getClient().getName() : null)
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .subtotal(invoice.getSubtotal())
                .cgstRate(invoice.getCgstRate())
                .sgstRate(invoice.getSgstRate())
                .igstRate(invoice.getIgstRate())
                .cgstAmount(invoice.getCgstAmount())
                .sgstAmount(invoice.getSgstAmount())
                .igstAmount(invoice.getIgstAmount())
                .totalAmount(invoice.getTotalAmount())
                .notes(invoice.getNotes())
                .terms(invoice.getTerms())
                .pdfUrl(invoice.getPdfUrl())
                .razorpayPaymentLinkUrl(invoice.getRazorpayPaymentLinkUrl())
                .items(invoice.getItems().stream().map(InvoiceItemResponse::from).toList())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}
