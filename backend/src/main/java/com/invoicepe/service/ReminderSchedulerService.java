package com.invoicepe.service;

import com.invoicepe.model.Invoice;
import com.invoicepe.model.ReminderLog;
import com.invoicepe.repository.InvoiceRepository;
import com.invoicepe.repository.ReminderLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Sends payment reminders and marks overdue invoices once a day.
 * Runs at 03:30 UTC == 09:00 IST.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSchedulerService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final String CHANNEL_WHATSAPP = "whatsapp";
    private static final String STATUS_SENT = "sent";
    private static final String STATUS_FAILED = "failed";
    private static final String INVOICE_SENT = "sent";
    private static final String INVOICE_OVERDUE = "overdue";

    static final String PRE_DUE = "pre_due";
    static final String ON_DUE = "on_due";
    static final String POST_DUE_3 = "post_due_3";
    static final String POST_DUE_7 = "post_due_7";

    private final InvoiceRepository invoiceRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final WhatsAppService whatsAppService;

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void runDailyReminders() {
        LocalDate today = LocalDate.now(IST);
        log.info("Running daily reminder job for {} (IST)", today);
        // Reminders run before marking overdue so post-due invoices are still 'sent'.
        processReminders(today);
        int marked = markOverdue(today);
        log.info("Daily reminder job complete; {} invoices marked overdue", marked);
    }

    void processReminders(LocalDate today) {
        List<String> sentOnly = List.of(INVOICE_SENT);
        List<String> sentOrOverdue = List.of(INVOICE_SENT, INVOICE_OVERDUE);
        sendForType(PRE_DUE, today.plusDays(3), sentOnly);
        sendForType(ON_DUE, today, sentOnly);
        sendForType(POST_DUE_3, today.minusDays(3), sentOrOverdue);
        sendForType(POST_DUE_7, today.minusDays(7), sentOrOverdue);
    }

    private void sendForType(String reminderType, LocalDate dueDate, List<String> statuses) {
        List<Invoice> invoices = invoiceRepository.findByStatusInAndDueDate(statuses, dueDate);
        for (Invoice invoice : invoices) {
            boolean alreadySent = reminderLogRepository
                    .existsByInvoiceIdAndReminderTypeAndStatus(invoice.getId(), reminderType, STATUS_SENT);
            if (alreadySent) {
                log.debug("Skipping duplicate {} reminder for invoice {}",
                        reminderType, invoice.getInvoiceNumber());
                continue;
            }
            sendReminder(invoice, reminderType);
        }
    }

    private void sendReminder(Invoice invoice, String reminderType) {
        String status = STATUS_SENT;
        String error = null;
        try {
            boolean delivered = whatsAppService.sendReminderMessage(invoice, reminderType);
            if (!delivered) {
                status = STATUS_FAILED;
                error = "WhatsApp delivery returned false";
            }
        } catch (Exception e) {
            // A WhatsApp failure must never crash the scheduler.
            status = STATUS_FAILED;
            error = e.getMessage();
            log.error("Reminder {} failed for invoice {}", reminderType, invoice.getInvoiceNumber(), e);
        }

        reminderLogRepository.save(ReminderLog.builder()
                .invoice(invoice)
                .reminderType(reminderType)
                .channel(CHANNEL_WHATSAPP)
                .status(status)
                .errorMessage(error)
                .build());
    }

    int markOverdue(LocalDate today) {
        List<Invoice> overdue = invoiceRepository.findByStatusAndDueDateBefore(INVOICE_SENT, today);
        for (Invoice invoice : overdue) {
            invoice.setStatus(INVOICE_OVERDUE);
        }
        invoiceRepository.saveAll(overdue);
        return overdue.size();
    }
}
