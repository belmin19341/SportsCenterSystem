package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.PaymentRepository;
import ba.nwt.paymentservice.saga.RefundSagaPublisher;
import ba.nwt.paymentservice.saga.event.BookingCancellationRequestedEvent;
import ba.nwt.paymentservice.saga.event.RefundCompletedEvent;
import ba.nwt.paymentservice.saga.event.RefundFailedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking Cancellation Saga — Payment Service side.
 *
 * Local TX 2: find the PAID payment for the booking and mark it as REFUNDED.
 * On success → publish RefundCompletedEvent → Booking finalised as CANCELLED.
 * On failure → publish RefundFailedEvent   → Booking restored to CONFIRMED (compensating TX).
 */
@Service
@RequiredArgsConstructor
public class RefundSagaService {

    private static final Logger log = LoggerFactory.getLogger(RefundSagaService.class);

    private final PaymentRepository paymentRepository;
    private final RefundSagaPublisher publisher;

    @Transactional
    public void processRefund(BookingCancellationRequestedEvent event) {
        log.info("[SAGA-CANCEL][{}] Local TX 2 start — processing refund for bookingId={}", event.getSagaId(), event.getBookingId());

        if (event.isSimulateFailure()) {
            log.warn("[SAGA-CANCEL][{}] Refund FAILED (simulated) for bookingId={}", event.getSagaId(), event.getBookingId());
            publisher.publishRefundFailed(RefundFailedEvent.builder()
                    .sagaId(event.getSagaId())
                    .bookingId(event.getBookingId())
                    .reason("Simulated refund failure for bookingId=" + event.getBookingId())
                    .timestamp(LocalDateTime.now())
                    .build());
            return;
        }

        try {
            List<Payment> payments = paymentRepository.findByBookingId(event.getBookingId());
            Payment payment = payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No PAID payment found for bookingId=" + event.getBookingId()));

            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            Payment saved = paymentRepository.save(payment);
            log.info("[SAGA-CANCEL][{}] Payment id={} REFUNDED — publishing RefundCompletedEvent", event.getSagaId(), saved.getId());

            publisher.publishRefundCompleted(RefundCompletedEvent.builder()
                    .sagaId(event.getSagaId())
                    .bookingId(event.getBookingId())
                    .paymentId(saved.getId())
                    .refundedAmount(saved.getAmount())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            log.error("[SAGA-CANCEL][{}] Refund processing error: {}", event.getSagaId(), ex.getMessage());
            publisher.publishRefundFailed(RefundFailedEvent.builder()
                    .sagaId(event.getSagaId())
                    .bookingId(event.getBookingId())
                    .reason(ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }
}
