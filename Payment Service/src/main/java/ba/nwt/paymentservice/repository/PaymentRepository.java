package ba.nwt.paymentservice.repository;

import ba.nwt.paymentservice.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBookingId(Long bookingId);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findByStatus(Payment.PaymentStatus status);

    @Query("""
           SELECT p FROM Payment p
           WHERE (:status IS NULL OR p.status = :status)
             AND (:method IS NULL OR p.paymentMethod = :method)
             AND (:bookingId IS NULL OR p.bookingId = :bookingId)
             AND (:minAmount IS NULL OR p.amount >= :minAmount)
             AND (:maxAmount IS NULL OR p.amount <= :maxAmount)
             AND (:from IS NULL OR p.createdAt >= :from)
             AND (:to IS NULL OR p.createdAt <= :to)
           """)
    Page<Payment> search(@Param("status") Payment.PaymentStatus status,
                         @Param("method") Payment.PaymentMethod method,
                         @Param("bookingId") Long bookingId,
                         @Param("minAmount") BigDecimal minAmount,
                         @Param("maxAmount") BigDecimal maxAmount,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         Pageable pageable);

    /** Total PAID revenue in the inclusive [from, to] window. */
    @Query("""
           SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
           WHERE p.status = :status
             AND p.paidAt BETWEEN :from AND :to
           """)
    BigDecimal sumRevenueBetweenForStatus(@Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("status") Payment.PaymentStatus status);

    default BigDecimal sumRevenueBetween(LocalDateTime from, LocalDateTime to) {
        return sumRevenueBetweenForStatus(from, to, Payment.PaymentStatus.PAID);
    }

    /** Aggregate of paid revenue per payment method in the inclusive [from, to] window. */
    @Query("""
           SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0), COUNT(p)
           FROM Payment p
           WHERE p.status = :status
             AND p.paidAt BETWEEN :from AND :to
           GROUP BY p.paymentMethod
           """)
    List<Object[]> revenueByMethodBetweenForStatus(@Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to,
                                                   @Param("status") Payment.PaymentStatus status);

    default List<Object[]> revenueByMethodBetween(LocalDateTime from, LocalDateTime to) {
        return revenueByMethodBetweenForStatus(from, to, Payment.PaymentStatus.PAID);
    }
}


