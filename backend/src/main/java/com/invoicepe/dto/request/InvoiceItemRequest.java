package com.invoicepe.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvoiceItemRequest {

    @NotBlank(message = "Item description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 10, message = "HSN/SAC code must not exceed 10 characters")
    private String hsnSacCode;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    private String unit;

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.00", message = "Rate must not be negative")
    private BigDecimal rate;

    private Integer displayOrder;
}
