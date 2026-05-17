package ba.nwt.paymentservice.saga;

import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.PaymentRepository;
import ba.nwt.paymentservice.saga.event.RentalCreatedEvent;
import ba.nwt.paymentservice.service.RentalPaymentSagaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalPaymentSagaServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock RentalPaymentSagaPublisher publisher;
    @InjectMocks RentalPaymentSagaService service;

    private RentalCreatedEvent baseEvent;

    @BeforeEach
    void setUp() {
        baseEvent = RentalCreatedEvent.builder()
                .sagaId("saga-rental-01").rentalId(20L).userId(1L).equipmentId(5L)
                .startDate(LocalDate.now().plusDays(1)).endDate(LocalDate.now().plusDays(3))
                .quantity(1).totalPrice(new BigDecimal("50.00"))
                .depositAmount(BigDecimal.ZERO).paymentMethod("CREDIT_CARD")
                .simulateFailure(false).timestamp(LocalDateTime.now()).build();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void processRentalPayment_happyPath_savesPendingThenPaid_andPublishesCompleted() {
        Payment pending = Payment.builder().id(55L).rentalId(20L)
                .status(Payment.PaymentStatus.PENDING).transactionId("TXN-RENTAL-AAA").build();

        when(paymentRepository.save(any())).thenReturn(pending)
                .thenReturn(Payment.builder().id(55L).status(Payment.PaymentStatus.PAID)
                        .transactionId("TXN-RENTAL-AAA").build());

        service.processRentalPayment(baseEvent);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(Payment.PaymentStatus.PAID);

        verify(publisher).publishCompleted(argThat(e -> e.getRentalId().equals(20L)));
        verify(publisher, never()).publishFailed(any());
    }

    @Test
    void processRentalPayment_transactionIdHasRentalPrefix() {
        when(paymentRepository.save(any())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        service.processRentalPayment(baseEvent);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getTransactionId()).startsWith("TXN-RENTAL-");
    }

    // ── Simulate failure (compensating) ──────────────────────────────────────

    @Test
    void processRentalPayment_withSimulateFailure_savesAsFailed_andPublishesFailed() {
        baseEvent.setSimulateFailure(true);
        Payment pending = Payment.builder().id(55L).status(Payment.PaymentStatus.PENDING).build();

        when(paymentRepository.save(any())).thenReturn(pending)
                .thenReturn(Payment.builder().id(55L).status(Payment.PaymentStatus.FAILED).build());

        service.processRentalPayment(baseEvent);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(1).getStatus()).isEqualTo(Payment.PaymentStatus.FAILED);

        verify(publisher).publishFailed(argThat(e -> e.getRentalId().equals(20L)));
        verify(publisher, never()).publishCompleted(any());
    }

    // ── Exception during processing ───────────────────────────────────────────

    @Test
    void processRentalPayment_onException_publishesFailed() {
        Payment pending = Payment.builder().id(55L).status(Payment.PaymentStatus.PENDING).build();
        when(paymentRepository.save(any()))
                .thenReturn(pending)
                .thenThrow(new RuntimeException("DB connection lost"))
                .thenReturn(Payment.builder().id(55L).status(Payment.PaymentStatus.FAILED).build());

        service.processRentalPayment(baseEvent);

        verify(publisher).publishFailed(argThat(e -> e.getReason().contains("DB connection lost")));
    }

    // ── PaymentMethod fallback ────────────────────────────────────────────────

    @Test
    void processRentalPayment_unknownPaymentMethod_defaultsToCreditCard() {
        baseEvent.setPaymentMethod("CRYPTO");
        when(paymentRepository.save(any())).thenAnswer(inv -> { Payment p = inv.getArgument(0); p.setId(1L); return p; });

        service.processRentalPayment(baseEvent);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getPaymentMethod()).isEqualTo(Payment.PaymentMethod.CREDIT_CARD);
    }
}
