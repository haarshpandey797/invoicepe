package com.invoicepe.service;

import com.invoicepe.dto.request.CreateInvoiceRequest;
import com.invoicepe.dto.request.InvoiceItemRequest;
import com.invoicepe.exception.InvoicePeException;
import com.invoicepe.exception.PlanLimitException;
import com.invoicepe.model.Client;
import com.invoicepe.model.Invoice;
import com.invoicepe.model.User;
import com.invoicepe.repository.ClientRepository;
import com.invoicepe.repository.InvoiceRepository;
import com.invoicepe.repository.PaymentRepository;
import com.invoicepe.util.GstCalculator;
import com.invoicepe.util.InvoiceNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanEnforcementTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private GstCalculator gstCalculator;
    @Mock private InvoiceNumberGenerator invoiceNumberGenerator;
    @Mock private PdfService pdfService;
    @Mock private StorageService storageService;
    @Mock private RazorpayService razorpayService;
    @Mock private WhatsAppService whatsAppService;
    @Mock private EmailService emailService;
    @InjectMocks private InvoiceService invoiceService;

    private User freeUser;
    private User proUser;
    private Client client;

    @BeforeEach
    void setUp() {
        freeUser = User.builder().id(UUID.randomUUID()).email("free@test.com")
                .fullName("Free User").plan("free").build();
        proUser = User.builder().id(UUID.randomUUID()).email("pro@test.com")
                .fullName("Pro User").plan("pro").build();
        client = Client.builder().id(UUID.randomUUID()).name("Test Client").build();
    }

    @Test
    void freePlan5thInvoiceAllowed() {
        when(invoiceRepository.countByUserIdAndYearAndMonth(eq(freeUser.getId()), anyInt(), anyInt()))
                .thenReturn(4L);
        when(clientRepository.findByIdAndUserId(client.getId(), freeUser.getId()))
                .thenReturn(Optional.of(client));
        when(invoiceNumberGenerator.generate(freeUser)).thenReturn("INV-2026-FRE-005");
        when(gstCalculator.calculate(any(), any(), any()))
                .thenReturn(new com.invoicepe.util.GstBreakdown(
                        BigDecimal.valueOf(1000), BigDecimal.valueOf(9), BigDecimal.valueOf(9),
                        BigDecimal.ZERO, BigDecimal.valueOf(90), BigDecimal.valueOf(90),
                        BigDecimal.ZERO, BigDecimal.valueOf(1180)));
        Invoice saved = Invoice.builder().invoiceNumber("INV-2026-FRE-005").status("draft")
                .user(freeUser).client(client).items(new ArrayList<>()).build();
        saved.setId(UUID.randomUUID());
        when(invoiceRepository.save(any())).thenReturn(saved);

        assertDoesNotThrow(() -> invoiceService.create(freeUser, createRequest()));
    }

    @Test
    void freePlan6thInvoiceThrowsPlanLimitException() {
        when(invoiceRepository.countByUserIdAndYearAndMonth(eq(freeUser.getId()), anyInt(), anyInt()))
                .thenReturn(5L);

        PlanLimitException ex = assertThrows(PlanLimitException.class,
                () -> invoiceService.create(freeUser, createRequest()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Free plan"));
    }

    @Test
    void proUserFacesNoInvoiceLimit() {
        when(clientRepository.findByIdAndUserId(client.getId(), proUser.getId()))
                .thenReturn(Optional.of(client));
        when(invoiceNumberGenerator.generate(proUser)).thenReturn("INV-2026-PRO-001");
        when(gstCalculator.calculate(any(), any(), any()))
                .thenReturn(new com.invoicepe.util.GstBreakdown(
                        BigDecimal.valueOf(1000), BigDecimal.valueOf(9), BigDecimal.valueOf(9),
                        BigDecimal.ZERO, BigDecimal.valueOf(90), BigDecimal.valueOf(90),
                        BigDecimal.ZERO, BigDecimal.valueOf(1180)));
        Invoice saved = Invoice.builder().invoiceNumber("INV-2026-PRO-001").status("draft")
                .user(proUser).client(client).items(new ArrayList<>()).build();
        saved.setId(UUID.randomUUID());
        when(invoiceRepository.save(any())).thenReturn(saved);

        assertDoesNotThrow(() -> invoiceService.create(proUser, createRequest()));
        verify(invoiceRepository, never()).countByUserIdAndYearAndMonth(any(), anyInt(), anyInt());
    }

    @Test
    void freePlanSendBlockedWith403() {
        InvoicePeException ex = assertThrows(InvoicePeException.class,
                () -> invoiceService.send(freeUser, UUID.randomUUID()));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getMessage().contains("Upgrade to Pro"));
    }

    @Test
    void proUserCanSendInvoice() {
        Invoice invoice = Invoice.builder().invoiceNumber("INV-2026-PRO-001").status("draft")
                .user(proUser).client(client).items(new ArrayList<>()).build();
        invoice.setId(UUID.randomUUID());
        when(invoiceRepository.findByIdAndUserId(invoice.getId(), proUser.getId()))
                .thenReturn(Optional.of(invoice));
        when(pdfService.generateInvoicePdf(any())).thenReturn(new byte[]{0x25, 0x50, 0x44, 0x46});
        when(storageService.uploadInvoicePdf(any(), any())).thenReturn(null);
        when(razorpayService.createPaymentLink(any()))
                .thenReturn(new PaymentLinkResult("link_id", "https://rzp.io/test"));
        when(invoiceRepository.save(any())).thenReturn(invoice);

        assertDoesNotThrow(() -> invoiceService.send(proUser, invoice.getId()));
    }

    private CreateInvoiceRequest createRequest() {
        InvoiceItemRequest item = new InvoiceItemRequest();
        item.setDescription("Service");
        item.setQuantity(BigDecimal.ONE);
        item.setRate(BigDecimal.valueOf(1000));

        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setClientId(client.getId());
        req.setIssueDate(LocalDate.now());
        req.setDueDate(LocalDate.now().plusDays(30));
        req.setItems(List.of(item));
        return req;
    }
}
