package ba.nwt.paymentservice.saga;

import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.PaymentRepository;
import ba.nwt.paymentservice.saga.event.BookingCreatedEvent;
import ba.nwt.paymentservice.saga.event.PaymentCompletedEvent;
import ba.nwt.paymentservice.saga.event.PaymentFailedEvent;
import ba.nwt.paymentservice.service.PaymentSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentSagaService — the Payment Service's side of the Saga.
 *
 * Tests cover:
 *  1. processPayment() happy path → saves Payment(PAID), publishes PaymentCompletedEvent
 *  2. processPayment() failure path → saves Payment(FAILED), publishes PaymentFailedEvent
 *  3. Correct transactionId format
 *  4. Correct paymentMethod mapping
 */
@ExtendWith(MockitoExtension.class)
class PaymentSagaServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentSagaPublisher publisher;

    @InjectMocks
    private PaymentSagaService paymentSagaService;

    private BookingCreatedEvent baseEvent;

    @BeforeEach
    void setUp() {
        baseEvent = BookingCreatedEvent.builder()
                .sagaId("saga-test-001")
                .bookingId(5L)
                .userId(1L)
                .facilityId(2L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .totalPrice(new BigDecimal("100.00"))
                .paymentMethod("CREDIT_CARD")
                .simulateFailure(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void processPayment_happyPath_savesPaymentAsPaid_andPublishesCompletedEvent() {
        Payment pendingPayment = Payment.builder()
                .id(99L)
                .bookingId(5L)
                .amount(new BigDecimal("100.00"))
                .status(Payment.PaymentStatus.PENDING)
                .transactionId("TXN-SAGA-ABC123")
                .build();

        // First save (PENDING), second save (PAID)
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(pendingPayment)   // first call
                .thenReturn(Payment.builder() // second call
                        .id(99L)
                        .bookingId(5L)
                        .amount(new BigDecimal("100.00"))
                        .status(Payment.PaymentStatus.PAID)
                        .transactionId("TXN-SAGA-ABC123")
                        .paidAt(LocalDateTime.now())
                        .build());

        paymentSagaService.processPayment(baseEvent);

        // Verify two saves: PENDING then PAID
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(Payment.PaymentStatus.PAID);

        // Verify PaymentCompletedEvent published
        verify(publisher).publishPaymentCompleted(argThat(event ->
                event.getSagaId().equals("saga-test-001")
                && event.getBookingId().equals(5L)
                && event.getPaymentId().equals(99L)
        ));
        verify(publisher, never()).publishPaymentFailed(any());
    }

    @Test
    void processPayment_happyPath_transactionIdHasSagaPrefix() {
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        paymentSagaService.processPayment(baseEvent);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());

        String txnId = captor.getAllValues().get(0).getTransactionId();
        assertThat(txnId).startsWith("TXN-SAGA-");
    }

    // ── Failure / compensating path ───────────────────────────────────────────

    @Test
    void processPayment_withSimulateFailure_savesPaymentAsFailed_andPublishesFailedEvent() {
        BookingCreatedEvent failEvent = BookingCreatedEvent.builder()
                .sagaId("saga-fail-001")
                .bookingId(7L)
                .totalPrice(new BigDecimal("200.00"))
                .paymentMethod("DEBIT_CARD")
                .simulateFailure(true)
                .timestamp(LocalDateTime.now())
                .build();

        Payment pendingPayment = Payment.builder()
                .id(55L)
                .bookingId(7L)
                .status(Payment.PaymentStatus.PENDING)
                .build();

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(pendingPayment)
                .thenReturn(Payment.builder().id(55L).status(Payment.PaymentStatus.FAILED).build());

        paymentSagaService.processPayment(failEvent);

        // Verify two saves: PENDING then FAILED
        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);

        // Verify PaymentFailedEvent published (compensating trigger)
        verify(publisher).publishPaymentFailed(argThat(event ->
                event.getSagaId().equals("saga-fail-001")
                && event.getBookingId().equals(7L)
                && event.getReason() != null
        ));
        verify(publisher, never()).publishPaymentCompleted(any());
    }

    @Test
    void processPayment_onUnexpectedException_publishesPaymentFailedEvent() {
        Payment pendingPayment = Payment.builder()
                .id(88L)
                .bookingId(5L)
                .status(Payment.PaymentStatus.PENDING)
                .transactionId("TXN-SAGA-ERR")
                .build();

        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(pendingPayment)
                // Second save (PAID attempt) throws
                .thenThrow(new RuntimeException("DB connection lost"))
                // Third save (FAILED)
                .thenReturn(Payment.builder().id(88L).status(Payment.PaymentStatus.FAILED).build());

        paymentSagaService.processPayment(baseEvent);

        verify(publisher).publishPaymentFailed(argThat(event ->
                event.getReason().contains("DB connection lost")
        ));
        verify(publisher, never()).publishPaymentCompleted(any());
    }

    // ── PaymentMethod mapping ─────────────────────────────────────────────────

    @Test
    void processPayment_unknownPaymentMethod_defaultsToCreditCard() {
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .sagaId("saga-method")
                .bookingId(3L)
                .totalPrice(BigDecimal.TEN)
                .paymentMethod("BITCOIN") // unknown
                .simulateFailure(false)
                .timestamp(LocalDateTime.now())
                .build();

        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        paymentSagaService.processPayment(event);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getPaymentMethod())
                .isEqualTo(Payment.PaymentMethod.CREDIT_CARD);
    }
}
