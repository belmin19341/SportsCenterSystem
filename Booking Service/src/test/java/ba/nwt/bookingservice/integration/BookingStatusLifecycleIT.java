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
 * Flow: životni ciklus statusa rezervacije.
 * Testira prijelaze između statusa i ispravno filtriranje po statusu.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingStatusLifecycleIT {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingUserRepository bookingUserRepository;
    @Autowired private EquipmentRentalRepository equipmentRentalRepository;

    @BeforeEach
    void cleanUp() {
        bookingUserRepository.deleteAll();
        equipmentRentalRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    private BookingResponseDTO createPendingBooking(int dayOffset) {
        LocalDateTime start = LocalDateTime.now().plusDays(dayOffset).withHour(10).withMinute(0).withSecond(0).withNano(0);
        return restTemplate.postForObject("/api/bookings",
                BookingRequestDTO.builder()
                        .userId(1L).facilityId(10L)
                        .startTime(start).endTime(start.plusHours(2))
                        .totalPrice(new BigDecimal("100.00"))
                        .status(Booking.BookingStatus.PENDING)
                        .build(),
                BookingResponseDTO.class);
    }

    private BookingResponseDTO updateStatus(Long id, Booking.BookingStatus newStatus,
                                            LocalDateTime start, LocalDateTime end) {
        ResponseEntity<BookingResponseDTO> resp = restTemplate.exchange(
                "/api/bookings/" + id, HttpMethod.PUT,
                new HttpEntity<>(BookingRequestDTO.builder()
                        .userId(1L).facilityId(10L)
                        .startTime(start).endTime(end)
                        .totalPrice(new BigDecimal("100.00"))
                        .status(newStatus).build()),
                BookingResponseDTO.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody();
    }

    @Test
    void pendingBooking_canBeConfirmed_statusPersistsInDatabase() {
        BookingResponseDTO booking = createPendingBooking(7);
        assertThat(booking.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);

        LocalDateTime start = booking.getStartTime();
        BookingResponseDTO confirmed = updateStatus(booking.getId(), Booking.BookingStatus.CONFIRMED, start, start.plusHours(2));

        assertThat(confirmed.getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
        // Verify directly in database
        assertThat(bookingRepository.findById(booking.getId()).get().getStatus())
                .isEqualTo(Booking.BookingStatus.CONFIRMED);
    }

    @Test
    void confirmedBooking_canBeCompleted_fullLifecycle() {
        BookingResponseDTO booking = createPendingBooking(8);
        LocalDateTime start = booking.getStartTime();

        // PENDING → CONFIRMED
        updateStatus(booking.getId(), Booking.BookingStatus.CONFIRMED, start, start.plusHours(2));

        // CONFIRMED → COMPLETED
        BookingResponseDTO completed = updateStatus(booking.getId(), Booking.BookingStatus.COMPLETED, start, start.plusHours(2));

        assertThat(completed.getStatus()).isEqualTo(Booking.BookingStatus.COMPLETED);
        assertThat(bookingRepository.findById(booking.getId()).get().getStatus())
                .isEqualTo(Booking.BookingStatus.COMPLETED);
    }

    @Test
    void cancelledBooking_doesNotAppearInPendingFilter() {
        BookingResponseDTO b1 = createPendingBooking(9);
        BookingResponseDTO b2 = createPendingBooking(10);
        LocalDateTime s1 = b1.getStartTime();

        // Cancel first
        updateStatus(b1.getId(), Booking.BookingStatus.CANCELLED, s1, s1.plusHours(2));

        ResponseEntity<List<BookingResponseDTO>> pending = restTemplate.exchange(
                "/api/bookings/status/PENDING", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<BookingResponseDTO>>() {});

        assertThat(pending.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pending.getBody()).hasSize(1);
        assertThat(pending.getBody().get(0).getId()).isEqualTo(b2.getId());
    }

    @Test
    void mixedStatusBookings_filterReturnsOnlyCorrectStatus() {
        BookingResponseDTO b1 = createPendingBooking(11);  // ostat će PENDING
        BookingResponseDTO b2 = createPendingBooking(12);  // ići u CONFIRMED
        BookingResponseDTO b3 = createPendingBooking(13);  // ići u COMPLETED
        LocalDateTime s2 = b2.getStartTime();
        LocalDateTime s3 = b3.getStartTime();

        updateStatus(b2.getId(), Booking.BookingStatus.CONFIRMED, s2, s2.plusHours(2));
        updateStatus(b3.getId(), Booking.BookingStatus.CONFIRMED, s3, s3.plusHours(2));
        updateStatus(b3.getId(), Booking.BookingStatus.COMPLETED, s3, s3.plusHours(2));

        ResponseEntity<List<BookingResponseDTO>> pending = restTemplate.exchange(
                "/api/bookings/status/PENDING", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<BookingResponseDTO>>() {});
        ResponseEntity<List<BookingResponseDTO>> confirmed = restTemplate.exchange(
                "/api/bookings/status/CONFIRMED", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<BookingResponseDTO>>() {});
        ResponseEntity<List<BookingResponseDTO>> completed = restTemplate.exchange(
                "/api/bookings/status/COMPLETED", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<BookingResponseDTO>>() {});

        assertThat(pending.getBody()).hasSize(1).allMatch(b -> b.getId().equals(b1.getId()));
        assertThat(confirmed.getBody()).hasSize(1).allMatch(b -> b.getId().equals(b2.getId()));
        assertThat(completed.getBody()).hasSize(1).allMatch(b -> b.getId().equals(b3.getId()));
    }
}
