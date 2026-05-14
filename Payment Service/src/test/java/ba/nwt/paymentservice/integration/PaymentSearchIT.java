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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Payment search and filter endpoints.
 * Validates filtering by status, method, bookingId, and amount range.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentSearchIT {

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

    private void createPayment(Long bookingId, BigDecimal amount,
                               Payment.PaymentMethod method, Payment.PaymentStatus status) {
        restTemplate.postForObject("/api/payments",
                PaymentRequestDTO.builder()
                        .bookingId(bookingId)
                        .amount(amount)
                        .depositAmount(BigDecimal.ZERO)
                        .paymentMethod(method)
                        .status(status)
                        .build(),
                PaymentResponseDTO.class);
    }

    @Test
    void searchByStatus_shouldReturnOnlyMatchingPayments() {
        createPayment(1L, new BigDecimal("100.00"), Payment.PaymentMethod.CREDIT_CARD, Payment.PaymentStatus.PAID);
        createPayment(2L, new BigDecimal("50.00"), Payment.PaymentMethod.PAYPAL, Payment.PaymentStatus.PENDING);
        createPayment(3L, new BigDecimal("75.00"), Payment.PaymentMethod.DEBIT_CARD, Payment.PaymentStatus.PAID);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/payments?status=PAID&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalElements")).isEqualTo(2);
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(2);
    }

    @Test
    void searchByMethod_shouldReturnOnlyPaymentsWithThatMethod() {
        createPayment(1L, new BigDecimal("100.00"), Payment.PaymentMethod.CREDIT_CARD, Payment.PaymentStatus.PENDING);
        createPayment(2L, new BigDecimal("80.00"), Payment.PaymentMethod.PAYPAL, Payment.PaymentStatus.PENDING);
        createPayment(3L, new BigDecimal("60.00"), Payment.PaymentMethod.CREDIT_CARD, Payment.PaymentStatus.PAID);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/payments?method=CREDIT_CARD&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalElements")).isEqualTo(2);
    }

    @Test
    void searchByBookingId_shouldReturnPaymentsForThatBooking() {
        createPayment(777L, new BigDecimal("50.00"), Payment.PaymentMethod.CREDIT_CARD, Payment.PaymentStatus.PENDING);
        createPayment(777L, new BigDecimal("25.00"), Payment.PaymentMethod.DEBIT_CARD, Payment.PaymentStatus.PAID);
        createPayment(888L, new BigDecimal("100.00"), Payment.PaymentMethod.PAYPAL, Payment.PaymentStatus.PENDING);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/payments?bookingId=777&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalElements")).isEqualTo(2);
    }

    @Test
    void searchByMinAmount_shouldReturnOnlyPaymentsAboveThreshold() {
        createPayment(1L, new BigDecimal("30.00"), Payment.PaymentMethod.CREDIT_CARD, Payment.PaymentStatus.PENDING);
        createPayment(2L, new BigDecimal("80.00"), Payment.PaymentMethod.CREDIT_CARD, Payment.PaymentStatus.PENDING);
        createPayment(3L, new BigDecimal("150.00"), Payment.PaymentMethod.CREDIT_CARD, Payment.PaymentStatus.PENDING);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/payments?minAmount=70.00&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalElements")).isEqualTo(2);
    }

    @Test
    void searchPaged_shouldRespectPageSizeAndTotalElements() {
        for (int i = 1; i <= 5; i++) {
            createPayment((long) i, new BigDecimal(i * 10 + ".00"),
                    Payment.PaymentMethod.CREDIT_CARD, Payment.PaymentStatus.PENDING);
        }

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/payments?method=CREDIT_CARD&page=0&size=3",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalElements")).isEqualTo(5);
        assertThat(response.getBody().get("totalPages")).isEqualTo(2);
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(3);
    }

    @Test
    void getAllPayments_withoutFilters_shouldReturnListOfAll() {
        createPayment(1L, new BigDecimal("50.00"), Payment.PaymentMethod.CREDIT_CARD, Payment.PaymentStatus.PENDING);
        createPayment(2L, new BigDecimal("75.00"), Payment.PaymentMethod.PAYPAL, Payment.PaymentStatus.PAID);

        ResponseEntity<List<PaymentResponseDTO>> response = restTemplate.exchange(
                "/api/payments",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<PaymentResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }
}
