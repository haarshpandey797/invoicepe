package com.invoicepe.repository;

import com.invoicepe.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByUserId(UUID userId, Pageable pageable);

    Page<Invoice> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);

    Optional<Invoice> findByIdAndUserId(UUID id, UUID userId);

    Optional<Invoice> findByRazorpayPaymentLinkId(String razorpayPaymentLinkId);

    boolean existsByInvoiceNumber(String invoiceNumber);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.user.id = :userId AND YEAR(i.createdAt) = :year")
    long countByUserIdAndYear(@Param("userId") UUID userId, @Param("year") int year);

    List<Invoice> findByStatusAndDueDate(String status, LocalDate dueDate);

    List<Invoice> findByStatusAndDueDateBefore(String status, LocalDate date);

    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId ORDER BY i.createdAt DESC LIMIT 5")
    List<Invoice> findTop5ByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
}
