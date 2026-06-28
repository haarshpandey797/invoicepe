package com.invoicepe.repository;

import com.invoicepe.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    List<Payment> findByInvoiceId(UUID invoiceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.user.id = :userId AND p.paidAt >= :start AND p.paidAt < :end")
    BigDecimal sumAmountByUserIdAndPaidAtBetween(@Param("userId") UUID userId,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    List<Payment> findByUserIdAndPaidAtGreaterThanEqual(UUID userId, LocalDateTime start);
}
