package ba.nwt.bookingservice.integration;

import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.repository.BookingUserRepository;
import ba.nwt.bookingservice.repository.EquipmentRentalRepository;
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
import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * WireMock integration tests for the synchronous Feign-based orchestration in
 * POST /api/bookings/orchestrated (BookingService#createOrchestrated).
 *
 * Unlike the unit-level mocks in BookingServiceZ5OrchestrationTest, these tests
 * exercise Feign's actual HTTP layer: URL construction, serialisation, deserialisation,
 * and the TypedErrorDecoder's HTTP-status-to-exception mapping.
 *
 * Feign clients are redirected to a local WireMock server via
 * spring.cloud.openfeign.client.config.<name>.url — Eureka is still disabled.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingFeignWireMockIT {

    // Single WireMock instance shared by all three downstream services.
    // Path prefixes (/api/facilities, /api/pricing-rules, /api/payments, /api/loyalty) are unique.
    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    // Redirect all three Feign clients to the WireMock server before the Spring context starts.
    @DynamicPropertySource
    static void overrideFeignUrls(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.openfeign.client.config.resource-service.url",
                () -> "http://localhost:" + wm.getPort());
        registry.add("spring.cloud.openfeign.client.config.payment-service.url",
                () -> "http://localhost:" + wm.getPort());
        registry.add("spring.cloud.openfeign.client.config.user-service.url",
                () -> "http://localhost:" + wm.getPort());
    }

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingUserRepository bookingUserRepository;

    @Autowired
    private EquipmentRentalRepository equipmentRentalRepository;

    @BeforeEach
    void cleanUp() {
        bookingUserRepository.deleteAll();
        equipmentRentalRepository.deleteAll();
        bookingRepository.deleteAll();
        wm.resetAll();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private BookingRequestDTO buildRequest() {
        LocalDateTime start = LocalDateTime.now().plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
        return BookingRequestDTO.builder()
                .userId(1L)
                .facilityId(1L)
                .startTime(start)
                .endTime(start.plusHours(2))
                .totalPrice(new BigDecimal("1.00"))
                .build();
    }

    private void stubAllServicesOk() {
        wm.stubFor(get(urlPathEqualTo("/api/facilities/1"))
                .willReturn(okJson(
                        "{\"id\":1,\"status\":\"ACTIVE\",\"name\":\"Tennis Court 1\",\"type\":\"TENNIS\"}")));

        wm.stubFor(get(urlPathEqualTo("/api/pricing-rules/calculate"))
                .willReturn(okJson(
                        "{\"facilityId\":1,\"totalPrice\":100.00,\"multiplier\":1.0,\"hours\":2.0}")));

        wm.stubFor(post(urlPathEqualTo("/api/payments"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"bookingId\":1,\"amount\":100.00," +
                                  "\"status\":\"PAID\",\"transactionId\":\"TXN-WIREMOCK-001\"}")));

        wm.stubFor(patch(urlPathMatching("/api/loyalty/user/\\d+/add-points"))
                .willReturn(ok()));
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    /**
     * All downstream services respond successfully.
     * Verifies that Feign actually fires all four HTTP requests and
     * that the booking ends up CONFIRMED in the database.
     */
    @Test
    void happyPath_allServicesOk_returns201AndBookingIsConfirmed() {
        stubAllServicesOk();

        ResponseEntity<BookingResponseDTO> response = restTemplate.postForEntity(
                "/api/bookings/orchestrated?paymentMethod=CREDIT_CARD",
                buildRequest(),
                BookingResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        List<Booking> bookings = bookingRepository.findAll();
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);

        // Feign really made all 3 critical HTTP calls — this is what @MockBean tests cannot verify.
        wm.verify(getRequestedFor(urlPathEqualTo("/api/facilities/1")));
        wm.verify(getRequestedFor(urlPathEqualTo("/api/pricing-rules/calculate")));
        wm.verify(postRequestedFor(urlPathEqualTo("/api/payments")));
        // Loyalty is best-effort: UserServiceClientFallback swallows failures silently.
        // The userServiceDown_loyaltyBestEffort_bookingStillConfirmed test covers that semantic.
    }

    /**
     * Resource Service returns 503.
     * TypedErrorDecoder maps it to DownstreamUnavailableException → GlobalExceptionHandler → 503.
     * The booking must NOT be persisted (failure happens before any DB write).
     */
    @Test
    void resourceServiceDown_returns503_noBookingPersisted() {
        wm.stubFor(get(urlPathEqualTo("/api/facilities/1"))
                .willReturn(aResponse().withStatus(503)));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/bookings/orchestrated?paymentMethod=CREDIT_CARD",
                buildRequest(),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(bookingRepository.findAll()).isEmpty();
    }

    /**
     * Resource Service returns an inactive facility (status != ACTIVE).
     * createOrchestrated throws IllegalArgumentException → GlobalExceptionHandler → 400.
     * No booking persisted.
     */
    @Test
    void facilityInactive_returns400_noBookingPersisted() {
        wm.stubFor(get(urlPathEqualTo("/api/facilities/1"))
                .willReturn(okJson(
                        "{\"id\":1,\"status\":\"INACTIVE\",\"name\":\"Court 1\",\"type\":\"TENNIS\"}")));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/bookings/orchestrated?paymentMethod=CREDIT_CARD",
                buildRequest(),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bookingRepository.findAll()).isEmpty();
    }

    /**
     * Payment Service returns 503 after the booking has been saved as PENDING.
     * DownstreamUnavailableException is a RuntimeException — @Transactional rolls back
     * the entire unit of work, so nothing survives in the database.
     * The caller receives 503.
     */
    @Test
    void paymentServiceDown_returns503_transactionRolledBack() {
        wm.stubFor(get(urlPathEqualTo("/api/facilities/1"))
                .willReturn(okJson(
                        "{\"id\":1,\"status\":\"ACTIVE\",\"name\":\"Court 1\",\"type\":\"TENNIS\"}")));
        wm.stubFor(get(urlPathEqualTo("/api/pricing-rules/calculate"))
                .willReturn(okJson(
                        "{\"facilityId\":1,\"totalPrice\":100.00}")));
        wm.stubFor(post(urlPathEqualTo("/api/payments"))
                .willReturn(aResponse().withStatus(503)));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/bookings/orchestrated?paymentMethod=CREDIT_CARD",
                buildRequest(),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        // @Transactional rolls back when DownstreamUnavailableException (RuntimeException) escapes.
        assertThat(bookingRepository.findAll()).isEmpty();
    }

    /**
     * User Service returns 503 while crediting loyalty points.
     * The fallback swallows the exception — booking must still be CONFIRMED.
     * This is the "best-effort" semantic: loyalty failure must never cancel a booking.
     */
    @Test
    void userServiceDown_loyaltyBestEffort_bookingStillConfirmed() {
        wm.stubFor(get(urlPathEqualTo("/api/facilities/1"))
                .willReturn(okJson(
                        "{\"id\":1,\"status\":\"ACTIVE\",\"name\":\"Court 1\",\"type\":\"TENNIS\"}")));
        wm.stubFor(get(urlPathEqualTo("/api/pricing-rules/calculate"))
                .willReturn(okJson(
                        "{\"facilityId\":1,\"totalPrice\":100.00}")));
        wm.stubFor(post(urlPathEqualTo("/api/payments"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"bookingId\":1,\"amount\":100.00," +
                                  "\"status\":\"PAID\",\"transactionId\":\"TXN-WIREMOCK-002\"}")));
        // User Service is down — 503
        wm.stubFor(patch(urlPathMatching("/api/loyalty/user/\\d+/add-points"))
                .willReturn(aResponse().withStatus(503)));

        ResponseEntity<BookingResponseDTO> response = restTemplate.postForEntity(
                "/api/bookings/orchestrated?paymentMethod=CREDIT_CARD",
                buildRequest(),
                BookingResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        List<Booking> bookings = bookingRepository.findAll();
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
    }
}
