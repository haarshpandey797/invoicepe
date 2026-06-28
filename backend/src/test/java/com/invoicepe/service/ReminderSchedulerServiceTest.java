package com.invoicepe.service;

import com.invoicepe.model.Invoice;
import com.invoicepe.model.ReminderLog;
import com.invoicepe.repository.InvoiceRepository;
import com.invoicepe.repository.ReminderLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ReminderLogRepository reminderLogRepository;
    @Mock
    private WhatsAppService whatsAppService;
    @InjectMocks
    private ReminderSchedulerService service;

    private final LocalDate today = LocalDate.of(2026, 6, 28);

    @Test
    void selectsInvoicesByDueDateForEachReminderType() {
        Invoice preDue = invoice();
        Invoice onDue = invoice();
        Invoice postDue3 = invoice();
        Invoice postDue7 = invoice();

        when(invoiceRepository.findByStatusAndDueDate("sent", today.plusDays(3))).thenReturn(List.of(preDue));
        when(invoiceRepository.findByStatusAndDueDate("sent", today)).thenReturn(List.of(onDue));
        when(invoiceRepository.findByStatusAndDueDate("sent", today.minusDays(3))).thenReturn(List.of(postDue3));
        when(invoiceRepository.findByStatusAndDueDate("sent", today.minusDays(7))).thenReturn(List.of(postDue7));
        when(reminderLogRepository.existsByInvoiceIdAndReminderTypeAndStatus(any(), any(), eq("sent")))
                .thenReturn(false);
        when(whatsAppService.sendReminderMessage(any(), any())).thenReturn(true);

        service.processReminders(today);

        verify(whatsAppService).sendReminderMessage(preDue, "pre_due");
        verify(whatsAppService).sendReminderMessage(onDue, "on_due");
        verify(whatsAppService).sendReminderMessage(postDue3, "post_due_3");
        verify(whatsAppService).sendReminderMessage(postDue7, "post_due_7");
    }

    @Test
    void skipsInvoiceWhenReminderAlreadySent() {
        Invoice onDue = invoice();
        when(invoiceRepository.findByStatusAndDueDate("sent", today.plusDays(3))).thenReturn(List.of());
        when(invoiceRepository.findByStatusAndDueDate("sent", today)).thenReturn(List.of(onDue));
        when(invoiceRepository.findByStatusAndDueDate("sent", today.minusDays(3))).thenReturn(List.of());
        when(invoiceRepository.findByStatusAndDueDate("sent", today.minusDays(7))).thenReturn(List.of());
        when(reminderLogRepository.existsByInvoiceIdAndReminderTypeAndStatus(onDue.getId(), "on_due", "sent"))
                .thenReturn(true);

        service.processReminders(today);

        verify(whatsAppService, never()).sendReminderMessage(any(), any());
        verify(reminderLogRepository, never()).save(any());
    }

    @Test
    void logsFailedReminderWithoutCrashingWhenWhatsAppThrows() {
        Invoice onDue = invoice();
        when(invoiceRepository.findByStatusAndDueDate("sent", today.plusDays(3))).thenReturn(List.of());
        when(invoiceRepository.findByStatusAndDueDate("sent", today)).thenReturn(List.of(onDue));
        when(invoiceRepository.findByStatusAndDueDate("sent", today.minusDays(3))).thenReturn(List.of());
        when(invoiceRepository.findByStatusAndDueDate("sent", today.minusDays(7))).thenReturn(List.of());
        when(reminderLogRepository.existsByInvoiceIdAndReminderTypeAndStatus(any(), any(), eq("sent")))
                .thenReturn(false);
        when(whatsAppService.sendReminderMessage(any(), any()))
                .thenThrow(new RuntimeException("WhatsApp API down"));

        assertDoesNotThrow(() -> service.processReminders(today));

        ArgumentCaptor<ReminderLog> captor = ArgumentCaptor.forClass(ReminderLog.class);
        verify(reminderLogRepository).save(captor.capture());
        assertEquals("failed", captor.getValue().getStatus());
    }

    @Test
    void marksSentInvoicesPastDueAsOverdue() {
        Invoice overdue = invoice();
        overdue.setStatus("sent");
        when(invoiceRepository.findByStatusAndDueDateBefore("sent", today))
                .thenReturn(new ArrayList<>(List.of(overdue)));

        int marked = service.markOverdue(today);

        assertEquals(1, marked);
        assertEquals("overdue", overdue.getStatus());
        verify(invoiceRepository).saveAll(anyList());
    }

    private Invoice invoice() {
        Invoice invoice = Invoice.builder().invoiceNumber("INV-TEST").status("sent").build();
        invoice.setId(UUID.randomUUID());
        return invoice;
    }
}
