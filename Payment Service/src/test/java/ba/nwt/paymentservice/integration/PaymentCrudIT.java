package ba.nwt.paymentservice.integration;

import ba.nwt.paymentservice.dto.PaymentRequestDTO;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Payment CRUD operations.
 * Payment service has no Feign calls — full HTTP + H2 stack, no mocks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentCrudIT {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    private PaymentRequestDTO buildRequest(Long bookingId, BigDecimal amount, Payment.PaymentMethod method) {
        return PaymentRequestDTO.builder()
                .bookingId(bookingId)
                .amount(amount)
                .depositAmount(BigDecimal.ZERO)
                .paymentMethod(method)
                .status(Payment.PaymentStatus.PENDING)
                .build();
    }

    @Test
    void createPayment_shouldReturn201AndPersistToDatabase() {
        PaymentRequestDTO request = buildRequest(100L, new BigDecimal("150.00"), Payment.PaymentMethod.CREDIT_CARD);

        ResponseEntity<PaymentResponseDTO> response =
                restTemplate.postForEntity("/api/payments", request, PaymentResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isPositive();
        assertThat(response.getBody().getBookingId()).isEqualTo(100L);
        assertThat(response.getBody().getAmount()).isEqualByComparingTo("150.00");
        assertThat(response.getBody().getPaymentMethod()).isEqualTo(Payment.PaymentMethod.CREDIT_CARD);
        assertThat(response.getBody().getStatus()).isEqualTo(Payment.PaymentStatus.PENDING);
        assertThat(response.getBody().getTransactionId()).isNotBlank();
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void getPaymentById_shouldReturn200WithPersistedData() {
        PaymentResponseDTO created = restTemplate.postForObject(
                "/api/payments", buildRequest(200L, new BigDecimal("80.00"), Payment.PaymentMethod.PAYPAL), PaymentResponseDTO.class);

        ResponseEntity<PaymentResponseDTO> response =
                restTemplate.getForEntity("/api/payments/" + created.getId(), PaymentResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getBookingId()).isEqualTo(200L);
        assertThat(response.getBody().getAmount()).isEqualByComparingTo("80.00");
        assertThat(response.getBody().getPaymentMethod()).isEqualTo(Payment.PaymentMethod.PAYPAL);
    }

    @Test
    void getPaymentsByBookingId_shouldReturnOnlyThatBookingsPayments() {
        restTemplate.postForObject("/api/payments", buildRequest(300L, new BigDecimal("50.00"), Payment.PaymentMethod.CREDIT_CARD), PaymentResponseDTO.class);
        restTemplate.postForObject("/api/payments", buildRequest(300L, new BigDecimal("25.00"), Payment.PaymentMethod.DEBIT_CARD), PaymentResponseDTO.class);
        restTemplate.postForObject("/api/payments", buildRequest(999L, new BigDecimal("75.00"), Payment.PaymentMethod.PAYPAL), PaymentResponseDTO.class);

        ResponseEntity<List<PaymentResponseDTO>> response = restTemplate.exchange(
                "/api/payments/booking/300",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<PaymentResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).allMatch(p -> p.getBookingId().equals(300L));
    }

    @Test
    void getPaymentsByStatus_shouldReturnOnlyMatchingStatus() {
        restTemplate.postForObject("/api/payments",
                PaymentRequestDTO.builder().bookingId(1L).amount(new BigDecimal("100.00"))
                        .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                        .status(Payment.PaymentStatus.PAID).build(),
                PaymentResponseDTO.class);
        restTemplate.postForObject("/api/payments",
                buildRequest(2L, new BigDecimal("50.00"), Payment.PaymentMethod.DEBIT_CARD),
                PaymentResponseDTO.class);

        ResponseEntity<List<PaymentResponseDTO>> response = restTemplate.exchange(
                "/api/payments/status/PAID",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<PaymentResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
    }

    @Test
    void updatePayment_shouldReturn200WithModifiedData() {
        PaymentResponseDTO created = restTemplate.postForObject(
                "/api/payments", buildRequest(400L, new BigDecimal("120.00"), Payment.PaymentMethod.CREDIT_CARD), PaymentResponseDTO.class);

        PaymentRequestDTO updateRequest = PaymentRequestDTO.builder()
                .bookingId(400L)
                .amount(new BigDecimal("120.00"))
                .paymentMethod(Payment.PaymentMethod.PAYPAL)
                .status(Payment.PaymentStatus.PAID)
                .build();

        ResponseEntity<PaymentResponseDTO> response = restTemplate.exchange(
                "/api/payments/" + created.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                PaymentResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(Payment.PaymentStatus.PAID);
        assertThat(response.getBody().getPaymentMethod()).isEqualTo(Payment.PaymentMethod.PAYPAL);
    }

    @Test
    void deletePayment_shouldReturn204AndSubsequentGetReturns404() {
        PaymentResponseDTO created = restTemplate.postForObject(
                "/api/payments", buildRequest(500L, new BigDecimal("60.00"), Payment.PaymentMethod.DEBIT_CARD), PaymentResponseDTO.class);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/payments/" + created.getId(), HttpMethod.DELETE, null, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<String> getResponse =
                restTemplate.getForEntity("/api/payments/" + created.getId(), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(paymentRepository.existsById(created.getId())).isFalse();
    }

    @Test
    void createPayment_shouldReturn400_whenAmountIsZero() {
        PaymentRequestDTO invalid = PaymentRequestDTO.builder()
                .bookingId(1L)
                .amount(new BigDecimal("0.00"))
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/payments", invalid, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(paymentRepository.count()).isZero();
    }

    @Test
    void createPayment_shouldReturn400_whenPaymentMethodIsMissing() {
        PaymentRequestDTO invalid = PaymentRequestDTO.builder()
                .bookingId(1L)
                .amount(new BigDecimal("50.00"))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/payments", invalid, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(paymentRepository.count()).isZero();
    }
}
