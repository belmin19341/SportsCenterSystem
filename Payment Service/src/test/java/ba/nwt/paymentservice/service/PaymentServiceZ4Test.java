package ba.nwt.paymentservice.service;

import ba.nwt.paymentservice.config.JsonPatchUtil;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
import ba.nwt.paymentservice.model.Notification;
import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.NotificationRepository;
import ba.nwt.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceZ4Test {

    @Mock private PaymentRepository paymentRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private JsonPatchUtil jsonPatchUtil;

    @InjectMocks private PaymentService paymentService;

    private Payment paid() {
        return Payment.builder().id(1L)
                .bookingId(10L).amount(new BigDecimal("50.00"))
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .transactionId("TXN-AAA").status(Payment.PaymentStatus.PAID)
                .paidAt(LocalDateTime.now()).build();
    }

    @Test
    void refund_paidPayment_marksRefundedAndPersistsNotification_atomically() {
        Payment p = paid();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(modelMapper.map(any(Payment.class), eq(PaymentResponseDTO.class)))
                .thenReturn(new PaymentResponseDTO());

        paymentService.refund(1L, 42L, "Customer request");

        ArgumentCaptor<Payment> pc = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(pc.capture());
        assertThat(pc.getValue().getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);

        ArgumentCaptor<Notification> nc = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(nc.capture());
        assertThat(nc.getValue().getUserId()).isEqualTo(42L);
        assertThat(nc.getValue().getType()).isEqualTo(Notification.NotificationType.PAYMENT_RECEIPT);
        assertThat(nc.getValue().getSubject()).contains("TXN-AAA");
        assertThat(nc.getValue().getMessage()).contains("Customer request");
    }

    @Test
    void refund_pendingPayment_throws_andDoesNotPersist() {
        Payment p = paid();
        p.setStatus(Payment.PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> paymentService.refund(1L, 42L, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PAID");

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void refund_missingRecipient_throws() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(paid()));
        assertThatThrownBy(() -> paymentService.refund(1L, null, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recipientUserId");
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void getRevenueBetween_aggregatesTotalAndPerMethodBreakdown() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        when(paymentRepository.sumRevenueBetween(eq(from), eq(to)))
                .thenReturn(new BigDecimal("250.00"));
        when(paymentRepository.revenueByMethodBetween(eq(from), eq(to)))
                .thenReturn(List.of(
                        new Object[]{Payment.PaymentMethod.CREDIT_CARD, new BigDecimal("200.00"), 4L},
                        new Object[]{Payment.PaymentMethod.PAYPAL, new BigDecimal("50.00"), 1L}));

        PaymentService.RevenueReport report = paymentService.getRevenueBetween(from, to);

        assertThat(report.getTotalRevenue()).isEqualByComparingTo("250.00");
        assertThat(report.getByMethod()).hasSize(2);
        assertThat(report.getByMethod().get(0).getMethod()).isEqualTo(Payment.PaymentMethod.CREDIT_CARD);
        assertThat(report.getByMethod().get(0).getCount()).isEqualTo(4L);
    }

    @Test
    void getRevenueBetween_invertedRange_throws() {
        LocalDateTime now = LocalDateTime.now();
        assertThatThrownBy(() -> paymentService.getRevenueBetween(now, now.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
