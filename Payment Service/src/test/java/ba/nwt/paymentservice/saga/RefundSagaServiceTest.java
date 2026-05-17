package ba.nwt.paymentservice.saga;

import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.PaymentRepository;
import ba.nwt.paymentservice.saga.event.BookingCancellationRequestedEvent;
import ba.nwt.paymentservice.saga.event.RefundCompletedEvent;
import ba.nwt.paymentservice.saga.event.RefundFailedEvent;
import ba.nwt.paymentservice.service.RefundSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundSagaServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock RefundSagaPublisher publisher;
    @InjectMocks RefundSagaService service;

    private Payment paidPayment;

    @BeforeEach
    void setUp() {
        paidPayment = Payment.builder()
                .id(99L).bookingId(10L).amount(new BigDecimal("150.00"))
                .status(Payment.PaymentStatus.PAID).transactionId("TXN-ABC").build();
    }

    private BookingCancellationRequestedEvent buildEvent(boolean simulateFailure) {
        return BookingCancellationRequestedEvent.builder()
                .sagaId("saga-cancel-01").bookingId(10L).userId(1L)
                .refundAmount(new BigDecimal("150.00"))
                .simulateFailure(simulateFailure).timestamp(LocalDateTime.now()).build();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void processRefund_happyPath_setsPaymentRefunded_andPublishesCompleted() {
        when(paymentRepository.findByBookingId(10L)).thenReturn(List.of(paidPayment));
        when(paymentRepository.save(any())).thenReturn(paidPayment);

        service.processRefund(buildEvent(false));

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);

        verify(publisher).publishRefundCompleted(argThat(e ->
                e.getSagaId().equals("saga-cancel-01") && e.getBookingId().equals(10L)));
        verify(publisher, never()).publishRefundFailed(any());
    }

    // ── Simulate failure (compensating) ──────────────────────────────────────

    @Test
    void processRefund_withSimulateFailure_publishesRefundFailed() {
        service.processRefund(buildEvent(true));

        verify(publisher).publishRefundFailed(argThat(e ->
                e.getBookingId().equals(10L) && e.getReason() != null));
        verify(publisher, never()).publishRefundCompleted(any());
        verifyNoInteractions(paymentRepository);
    }

    // ── No PAID payment ───────────────────────────────────────────────────────

    @Test
    void processRefund_noPaidPayment_publishesRefundFailed() {
        paidPayment.setStatus(Payment.PaymentStatus.PENDING);
        when(paymentRepository.findByBookingId(10L)).thenReturn(List.of(paidPayment));

        service.processRefund(buildEvent(false));

        verify(publisher).publishRefundFailed(argThat(e ->
                e.getBookingId().equals(10L) && e.getReason().contains("No PAID payment")));
        verify(publisher, never()).publishRefundCompleted(any());
    }

    @Test
    void processRefund_noPaymentAtAll_publishesRefundFailed() {
        when(paymentRepository.findByBookingId(10L)).thenReturn(List.of());

        service.processRefund(buildEvent(false));

        verify(publisher).publishRefundFailed(any());
        verify(publisher, never()).publishRefundCompleted(any());
    }

    // ── Repository exception ──────────────────────────────────────────────────

    @Test
    void processRefund_onRepositoryException_publishesRefundFailed() {
        when(paymentRepository.findByBookingId(10L)).thenThrow(new RuntimeException("DB error"));

        service.processRefund(buildEvent(false));

        verify(publisher).publishRefundFailed(argThat(e -> e.getReason().contains("DB error")));
    }
}
