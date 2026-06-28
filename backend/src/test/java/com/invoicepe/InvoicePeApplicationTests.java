package com.invoicepe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = InvoicePeApplication.class)
@ActiveProfiles("dev")
class InvoicePeApplicationTests {

    @Test
    void contextLoads() {
    }
}
