package ba.nwt.bookingservice.integration;

import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.repository.BookingUserRepository;
import ba.nwt.bookingservice.repository.EquipmentRentalRepository;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Booking CRUD operations.
 * POST /api/bookings is fully local (no Feign). Only /orchestrated uses Feign.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingCrudIT {

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
    }

    private BookingRequestDTO buildRequest(Long userId, Long facilityId) {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        return BookingRequestDTO.builder()
                .userId(userId)
                .facilityId(facilityId)
                .startTime(start)
                .endTime(end)
                .totalPrice(new BigDecimal("100.00"))
                .isRecurring(false)
                .status(Booking.BookingStatus.PENDING)
                .build();
    }

    @Test
    void createBooking_shouldReturn201AndPersistToDatabase() {
        BookingRequestDTO request = buildRequest(1L, 10L);

        ResponseEntity<BookingResponseDTO> response =
                restTemplate.postForEntity("/api/bookings", request, BookingResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isPositive();
        assertThat(response.getBody().getUserId()).isEqualTo(1L);
        assertThat(response.getBody().getFacilityId()).isEqualTo(10L);
        assertThat(response.getBody().getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
        assertThat(response.getBody().getTotalPrice()).isEqualByComparingTo("100.00");
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void getBookingById_shouldReturn200WithPersistedData() {
        BookingResponseDTO created =
                restTemplate.postForObject("/api/bookings", buildRequest(2L, 20L), BookingResponseDTO.class);

        ResponseEntity<BookingResponseDTO> response =
                restTemplate.getForEntity("/api/bookings/" + created.getId(), BookingResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUserId()).isEqualTo(2L);
        assertThat(response.getBody().getFacilityId()).isEqualTo(20L);
    }

    @Test
    void getBookingsByUserId_shouldReturnOnlyThatUsersBookings() {
        restTemplate.postForObject("/api/bookings", buildRequest(5L, 10L), BookingResponseDTO.class);
        restTemplate.postForObject("/api/bookings", buildRequest(5L, 11L), BookingResponseDTO.class);
        restTemplate.postForObject("/api/bookings", buildRequest(9L, 10L), BookingResponseDTO.class);

        ResponseEntity<List<BookingResponseDTO>> response = restTemplate.exchange(
                "/api/bookings/user/5",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<BookingResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).allMatch(b -> b.getUserId().equals(5L));
    }

    @Test
    void getBookingsByStatus_shouldReturnOnlyMatchingStatus() {
        restTemplate.postForObject("/api/bookings", buildRequest(1L, 10L), BookingResponseDTO.class);
        restTemplate.postForObject("/api/bookings", buildRequest(2L, 10L), BookingResponseDTO.class);

        // Directly update one booking to CONFIRMED via repository to test status filtering
        bookingRepository.findAll().stream().limit(1).forEach(b -> {
            b.setStatus(Booking.BookingStatus.CONFIRMED);
            bookingRepository.save(b);
        });

        ResponseEntity<List<BookingResponseDTO>> response = restTemplate.exchange(
                "/api/bookings/status/PENDING",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<List<BookingResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
    }

    @Test
    void updateBooking_shouldReturn200WithModifiedData() {
        BookingResponseDTO created =
                restTemplate.postForObject("/api/bookings", buildRequest(1L, 10L), BookingResponseDTO.class);

        LocalDateTime newStart = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
        BookingRequestDTO updateRequest = BookingRequestDTO.builder()
                .userId(1L)
                .facilityId(10L)
                .startTime(newStart)
                .endTime(newStart.plusHours(1))
                .totalPrice(new BigDecimal("75.00"))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        ResponseEntity<BookingResponseDTO> response = restTemplate.exchange(
                "/api/bookings/" + created.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                BookingResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
        assertThat(response.getBody().getTotalPrice()).isEqualByComparingTo("75.00");
    }

    @Test
    void deleteBooking_shouldReturn204AndSubsequentGetReturns404() {
        BookingResponseDTO created =
                restTemplate.postForObject("/api/bookings", buildRequest(1L, 10L), BookingResponseDTO.class);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/bookings/" + created.getId(), HttpMethod.DELETE, null, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<String> getResponse =
                restTemplate.getForEntity("/api/bookings/" + created.getId(), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(bookingRepository.existsById(created.getId())).isFalse();
    }

    @Test
    void createBooking_shouldReturn400_whenEndTimeIsBeforeStartTime() {
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        BookingRequestDTO invalid = BookingRequestDTO.builder()
                .userId(1L)
                .facilityId(10L)
                .startTime(start)
                .endTime(start.minusHours(1))   // end < start
                .totalPrice(new BigDecimal("50.00"))
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/bookings", invalid, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bookingRepository.count()).isZero();
    }
}
