package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.PaymentRepository;
import ba.nwt.paymentservice.saga.PaymentSagaPublisher;
import ba.nwt.paymentservice.saga.event.BookingCreatedEvent;
import ba.nwt.paymentservice.saga.event.PaymentCompletedEvent;
import ba.nwt.paymentservice.saga.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrates the Payment side of the Booking-Payment Saga.
 *
 * Saga steps owned by this service:
 *  1. processPayment() — save Payment(PENDING), attempt charge, update to PAID or FAILED
 *                        [local transaction 2]
 *     → On success: publish PaymentCompletedEvent
 *     → On failure: publish PaymentFailedEvent  (triggers compensating tx in Booking Service)
 */
@Service
@RequiredArgsConstructor
public class PaymentSagaService {

    private static final Logger log = LoggerFactory.getLogger(PaymentSagaService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentSagaPublisher publisher;

    /**
     * Local Transaction 2: create payment record and attempt processing.
     *
     * If simulateFailure=true, the payment is immediately marked FAILED
     * (used to test the compensating transaction path in Booking Service).
     */
    @Transactional
    public void processPayment(BookingCreatedEvent event) {
        log.info("[SAGA][{}] Local txn 2 start — processing payment for bookingId={}",
                event.getSagaId(), event.getBookingId());

        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(event.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            method = Payment.PaymentMethod.CREDIT_CARD;
        }

        // ── Save Payment PENDING ───────────────────────────────────────────────
        Payment payment = paymentRepository.save(Payment.builder()
                .bookingId(event.getBookingId())
                .amount(event.getTotalPrice())
                .depositAmount(java.math.BigDecimal.ZERO)
                .paymentMethod(method)
                .transactionId("TXN-SAGA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(Payment.PaymentStatus.PENDING)
                .build());

        log.info("[SAGA][{}] Payment saved id={} status=PENDING transactionId={}",
                event.getSagaId(), payment.getId(), payment.getTransactionId());

        // ── Simulate failure path (for testing) ───────────────────────────────
        if (event.isSimulateFailure()) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("[SAGA][{}] Payment id={} FAILED (simulated) — publishing PaymentFailedEvent",
                    event.getSagaId(), payment.getId());
            publisher.publishPaymentFailed(PaymentFailedEvent.builder()
                    .sagaId(event.getSagaId())
                    .bookingId(event.getBookingId())
                    .reason("Simulated payment failure for bookingId=" + event.getBookingId())
                    .timestamp(LocalDateTime.now())
                    .build());
            return;
        }

        // ── Happy path: mark payment as PAID ──────────────────────────────────
        try {
            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            Payment saved = paymentRepository.save(payment);
            log.info("[SAGA][{}] Payment id={} PAID — publishing PaymentCompletedEvent",
                    event.getSagaId(), saved.getId());
            publisher.publishPaymentCompleted(PaymentCompletedEvent.builder()
                    .sagaId(event.getSagaId())
                    .bookingId(event.getBookingId())
                    .paymentId(saved.getId())
                    .transactionId(saved.getTransactionId())
                    .amount(saved.getAmount())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            // Unexpected error during payment processing — compensate
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.error("[SAGA][{}] Unexpected error during payment processing: {}",
                    event.getSagaId(), ex.getMessage());
            publisher.publishPaymentFailed(PaymentFailedEvent.builder()
                    .sagaId(event.getSagaId())
                    .bookingId(event.getBookingId())
                    .reason("Internal payment error: " + ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }
}
