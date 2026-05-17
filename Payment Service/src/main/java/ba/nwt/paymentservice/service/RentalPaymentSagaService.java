package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.PaymentRepository;
import ba.nwt.paymentservice.saga.RentalPaymentSagaPublisher;
import ba.nwt.paymentservice.saga.event.RentalCreatedEvent;
import ba.nwt.paymentservice.saga.event.RentalPaymentCompletedEvent;
import ba.nwt.paymentservice.saga.event.RentalPaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Equipment Rental Saga — Payment Service side.
 *
 * Local TX 2: create and process payment for the rental.
 * On success → publish RentalPaymentCompletedEvent → Rental confirmed.
 * On failure → publish RentalPaymentFailedEvent   → Rental cancelled (compensating TX).
 */
@Service
@RequiredArgsConstructor
public class RentalPaymentSagaService {

    private static final Logger log = LoggerFactory.getLogger(RentalPaymentSagaService.class);

    private final PaymentRepository paymentRepository;
    private final RentalPaymentSagaPublisher publisher;

    @Transactional
    public void processRentalPayment(RentalCreatedEvent event) {
        log.info("[SAGA-RENTAL][{}] Local TX 2 start — processing payment for rentalId={}", event.getSagaId(), event.getRentalId());

        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(event.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            method = Payment.PaymentMethod.CREDIT_CARD;
        }

        Payment payment = paymentRepository.save(Payment.builder()
                .rentalId(event.getRentalId())
                .amount(event.getTotalPrice())
                .depositAmount(event.getDepositAmount() != null ? event.getDepositAmount() : java.math.BigDecimal.ZERO)
                .paymentMethod(method)
                .transactionId("TXN-RENTAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(Payment.PaymentStatus.PENDING)
                .build());

        if (event.isSimulateFailure()) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("[SAGA-RENTAL][{}] Payment FAILED (simulated) for rentalId={}", event.getSagaId(), event.getRentalId());
            publisher.publishFailed(RentalPaymentFailedEvent.builder()
                    .sagaId(event.getSagaId())
                    .rentalId(event.getRentalId())
                    .reason("Simulated payment failure for rentalId=" + event.getRentalId())
                    .timestamp(LocalDateTime.now())
                    .build());
            return;
        }

        try {
            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            Payment saved = paymentRepository.save(payment);
            log.info("[SAGA-RENTAL][{}] Payment id={} PAID for rentalId={}", event.getSagaId(), saved.getId(), event.getRentalId());
            publisher.publishCompleted(RentalPaymentCompletedEvent.builder()
                    .sagaId(event.getSagaId())
                    .rentalId(event.getRentalId())
                    .paymentId(saved.getId())
                    .transactionId(saved.getTransactionId())
                    .amount(saved.getAmount())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.error("[SAGA-RENTAL][{}] Unexpected error during rental payment: {}", event.getSagaId(), ex.getMessage());
            publisher.publishFailed(RentalPaymentFailedEvent.builder()
                    .sagaId(event.getSagaId())
                    .rentalId(event.getRentalId())
                    .reason("Internal error: " + ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }
}
