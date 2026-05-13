package ba.nwt.paymentservice.integration;

import ba.nwt.paymentservice.dto.PaymentRequestDTO;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
import ba.nwt.paymentservice.model.Notification;
import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.NotificationRepository;
import ba.nwt.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flow: životni ciklus plaćanja — PENDING → PAID → REFUNDED.
 * Testira da refund mijenja status, da se kreira notifikacija,
 * i da se ne može refundovati plaćanje koje nije u statusu PAID.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentLifecycleIT {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    private PaymentResponseDTO createPayment(Payment.PaymentStatus status) {
        return restTemplate.postForObject("/api/payments",
                PaymentRequestDTO.builder()
                        .bookingId(100L)
                        .amount(new BigDecimal("150.00"))
                        .depositAmount(BigDecimal.ZERO)
                        .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                        .status(status)
                        .build(),
                PaymentResponseDTO.class);
    }

    private PaymentResponseDTO updatePayment(Long id, Payment.PaymentStatus newStatus) {
        ResponseEntity<PaymentResponseDTO> resp = restTemplate.exchange(
                "/api/payments/" + id, HttpMethod.PUT,
                new HttpEntity<>(PaymentRequestDTO.builder()
                        .bookingId(100L)
                        .amount(new BigDecimal("150.00"))
                        .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                        .status(newStatus).build()),
                PaymentResponseDTO.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    @Test
    void createPaidPayment_shouldHavePaidAtTimestampSet() {
        PaymentResponseDTO payment = createPayment(Payment.PaymentStatus.PAID);

        assertThat(payment.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
    }

    @Test
    void paidPayment_canBeRefunded_statusChangesToRefunded() {
        PaymentResponseDTO payment = createPayment(Payment.PaymentStatus.PAID);

        String url = UriComponentsBuilder.fromPath("/api/payments/{id}/refund")
                .queryParam("recipientUserId", 5L)
                .queryParam("reason", "Customer request")
                .buildAndExpand(payment.getId()).toUriString();

        ResponseEntity<PaymentResponseDTO> resp = restTemplate.exchange(
                url, HttpMethod.POST, null, PaymentResponseDTO.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        // Provjeri u bazi
        assertThat(paymentRepository.findById(payment.getId()).get().getStatus())
                .isEqualTo(Payment.PaymentStatus.REFUNDED);
    }

    @Test
    void refund_shouldCreateNotificationWithTransactionIdInSubject() {
        PaymentResponseDTO payment = createPayment(Payment.PaymentStatus.PAID);
        assertThat(notificationRepository.count()).isZero();

        String url = UriComponentsBuilder.fromPath("/api/payments/{id}/refund")
                .queryParam("recipientUserId", 7L)
                .queryParam("reason", "Duplicate payment")
                .buildAndExpand(payment.getId()).toUriString();
        restTemplate.exchange(url, HttpMethod.POST, null, PaymentResponseDTO.class);

        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getUserId()).isEqualTo(7L);
        assertThat(notifications.get(0).getSubject()).contains(payment.getTransactionId());
        assertThat(notifications.get(0).getMessage()).contains("Duplicate payment");
    }

    @Test
    void pendingPayment_cannotBeRefunded_returns400() {
        PaymentResponseDTO payment = createPayment(Payment.PaymentStatus.PENDING);

        String url = UriComponentsBuilder.fromPath("/api/payments/{id}/refund")
                .queryParam("recipientUserId", 1L)
                .buildAndExpand(payment.getId()).toUriString();
        ResponseEntity<String> resp = restTemplate.exchange(
                url, HttpMethod.POST, null, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Status nije promijenjen
        assertThat(paymentRepository.findById(payment.getId()).get().getStatus())
                .isEqualTo(Payment.PaymentStatus.PENDING);
        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    void updatePendingToPaid_thenRefund_fullLifecycle() {
        // Kreiraj kao PENDING
        PaymentResponseDTO payment = createPayment(Payment.PaymentStatus.PENDING);
        assertThat(payment.getPaidAt()).isNull();

        // Ažuriraj na PAID
        PaymentResponseDTO paid = updatePayment(payment.getId(), Payment.PaymentStatus.PAID);
        assertThat(paid.getStatus()).isEqualTo(Payment.PaymentStatus.PAID);

        // Refunduj
        String url = UriComponentsBuilder.fromPath("/api/payments/{id}/refund")
                .queryParam("recipientUserId", 3L)
                .buildAndExpand(payment.getId()).toUriString();
        ResponseEntity<PaymentResponseDTO> refunded = restTemplate.exchange(
                url, HttpMethod.POST, null, PaymentResponseDTO.class);

        assertThat(refunded.getBody().getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
        assertThat(notificationRepository.count()).isEqualTo(1);
    }
}
