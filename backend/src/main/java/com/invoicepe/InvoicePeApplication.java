package com.invoicepe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvoicePeApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoicePeApplication.class, args);
    }
}
