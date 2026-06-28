package com.invoicepe.repository;

import com.invoicepe.model.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, UUID> {

    /**
     * A reminder of this type was already successfully sent for the invoice.
     * Used to skip duplicates (failed attempts are allowed to retry).
     */
    boolean existsByInvoiceIdAndReminderTypeAndStatus(UUID invoiceId, String reminderType, String status);
}
