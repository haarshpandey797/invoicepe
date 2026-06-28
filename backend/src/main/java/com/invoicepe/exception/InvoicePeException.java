package com.invoicepe.exception;

import org.springframework.http.HttpStatus;

public class InvoicePeException extends RuntimeException {

    private final HttpStatus status;

    public InvoicePeException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public InvoicePeException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
