package com.invoicepe.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends InvoicePeException {

    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found: " + id, HttpStatus.NOT_FOUND);
    }
}
