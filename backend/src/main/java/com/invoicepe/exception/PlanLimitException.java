package com.invoicepe.exception;

import org.springframework.http.HttpStatus;

public class PlanLimitException extends InvoicePeException {

    public PlanLimitException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
