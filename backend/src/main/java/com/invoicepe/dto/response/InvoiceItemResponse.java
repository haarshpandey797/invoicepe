package com.invoicepe.dto.response;

import com.invoicepe.model.InvoiceItem;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class InvoiceItemResponse {

    private UUID id;
    private String description;
    private String hsnSacCode;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal rate;
    private BigDecimal amount;
    private Integer displayOrder;

    public static InvoiceItemResponse from(InvoiceItem item) {
        return InvoiceItemResponse.builder()
                .id(item.getId())
                .description(item.getDescription())
                .hsnSacCode(item.getHsnSacCode())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .rate(item.getRate())
                .amount(item.getAmount())
                .displayOrder(item.getDisplayOrder())
                .build();
    }
}
