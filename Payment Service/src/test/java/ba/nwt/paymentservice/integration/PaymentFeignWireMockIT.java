package ba.nwt.paymentservice.integration;

import ba.nwt.paymentservice.dto.PaymentRequestDTO;
import ba.nwt.paymentservice.dto.PaymentResponseDTO;
import ba.nwt.paymentservice.model.Payment;
import ba.nwt.paymentservice.repository.PaymentRepository;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * WireMock integration tests for synchronous Payment → User Service communication.
 *
 * Business rule: before persisting a payment, Payment Service synchronously calls
 * User Service (GET /api/users/{id}) to confirm the paying user exists.
 *
 * Test levels covered here:
 *   - Happy path: user exists → payment created 201
 *   - User not found (404 from User Service) → payment rejected 400
 *   - User Service down (503 from User Service) → payment rejected 503
 *   - No userId provided → validation skipped, payment created normally
 *
 * The Feign client (UserServiceClient) is redirected to a local WireMock server via
 * spring.cloud.openfeign.client.config.user-service.url so no real service is needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentFeignWireMockIT {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void overrideFeignUrls(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.user-service.url",
                () -> "http://localhost:" + wm.getPort());
    }

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void cleanUp() {
        paymentRepository.deleteAll();
        wm.resetAll();
    }

    private PaymentRequestDTO buildRequest(Long userId) {
        return PaymentRequestDTO.builder()
                .userId(userId)
                .bookingId(1L)
                .amount(new BigDecimal("50.00"))
                .paymentMethod(Payment.PaymentMethod.CREDIT_CARD)
                .status(Payment.PaymentStatus.PAID)
                .build();
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    /**
     * User Service confirms the user exists (200).
     * Payment Service creates the payment and returns 201.
     * Verifies that the GET /api/users/{id} call was actually made via Feign.
     */
    @Test
    void userExists_paymentCreated_returns201() {
        wm.stubFor(get(urlPathEqualTo("/api/users/1"))
                .willReturn(okJson(
                        "{\"id\":1,\"username\":\"john\",\"email\":\"john@example.com\",\"role\":\"USER\"}")));

        ResponseEntity<PaymentResponseDTO> response = restTemplate.postForEntity(
                "/api/payments", buildRequest(1L), PaymentResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(Payment.PaymentStatus.PAID);

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);

        // Feign really sent GET /api/users/1 to User Service
        wm.verify(getRequestedFor(urlPathEqualTo("/api/users/1")));
    }

    /**
     * User Service returns 404 — the user does not exist.
     * TypedErrorDecoder maps 404 → DownstreamBadRequestException.
     * GlobalExceptionHandler maps it → HTTP 400.
     * No payment must be persisted.
     */
    @Test
    void userNotFound_returns400_noPaymentPersisted() {
        wm.stubFor(get(urlPathEqualTo("/api/users/99"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"User not found with id: 99\"}")));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/payments", buildRequest(99L), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(paymentRepository.findAll()).isEmpty();
    }

    /**
     * User Service returns 503 — service is down.
     * TypedErrorDecoder maps 5xx → DownstreamUnavailableException.
     * GlobalExceptionHandler maps it → HTTP 503.
     * No payment must be persisted.
     */
    @Test
    void userServiceDown_returns503_noPaymentPersisted() {
        wm.stubFor(get(urlPathEqualTo("/api/users/1"))
                .willReturn(aResponse().withStatus(503)));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/payments", buildRequest(1L), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(paymentRepository.findAll()).isEmpty();
    }

    /**
     * No userId in the request — validation is skipped entirely.
     * Payment is created normally without any call to User Service.
     */
    @Test
    void noUserId_validationSkipped_paymentCreated_returns201() {
        ResponseEntity<PaymentResponseDTO> response = restTemplate.postForEntity(
                "/api/payments", buildRequest(null), PaymentResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(paymentRepository.findAll()).hasSize(1);

        // No call to User Service was made
        wm.verify(0, getRequestedFor(urlPathMatching("/api/users/.*")));
    }
}
