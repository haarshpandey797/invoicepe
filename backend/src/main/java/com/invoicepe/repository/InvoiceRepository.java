package com.invoicepe.repository;

import com.invoicepe.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByUserId(UUID userId, Pageable pageable);

    Page<Invoice> findByUserIdAndStatus(UUID userId, String status, Pageable pageable);

    /**
     * Lists a user's invoices with optional status and client-name search filters.
     * Passing {@code null} for a filter disables it.
     */
    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId " +
            "AND (:status IS NULL OR i.status = :status) " +
            "AND (:search IS NULL OR LOWER(i.client.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Invoice> searchForUser(@Param("userId") UUID userId,
                                @Param("status") String status,
                                @Param("search") String search,
                                Pageable pageable);

    Optional<Invoice> findByIdAndUserId(UUID id, UUID userId);

    Optional<Invoice> findByRazorpayPaymentLinkId(String razorpayPaymentLinkId);

    boolean existsByInvoiceNumber(String invoiceNumber);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.user.id = :userId AND YEAR(i.createdAt) = :year")
    long countByUserIdAndYear(@Param("userId") UUID userId, @Param("year") int year);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.user.id = :userId AND YEAR(i.createdAt) = :year AND MONTH(i.createdAt) = :month")
    long countByUserIdAndYearAndMonth(@Param("userId") UUID userId, @Param("year") int year, @Param("month") int month);

    List<Invoice> findByStatusAndDueDate(String status, LocalDate dueDate);

    List<Invoice> findByStatusInAndDueDate(List<String> statuses, LocalDate dueDate);

    List<Invoice> findByStatusAndDueDateBefore(String status, LocalDate date);

    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId ORDER BY i.createdAt DESC LIMIT 5")
    List<Invoice> findTop5ByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i " +
            "WHERE i.user.id = :userId AND i.status = :status")
    BigDecimal sumTotalAmountByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") String status);

    @Query("SELECT i.status, COUNT(i) FROM Invoice i WHERE i.user.id = :userId GROUP BY i.status")
    List<Object[]> countByStatusForUser(@Param("userId") UUID userId);
}
