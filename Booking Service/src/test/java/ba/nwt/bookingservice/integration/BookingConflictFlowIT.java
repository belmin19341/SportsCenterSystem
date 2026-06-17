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
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flow: conflict detection between bookings.
 * Tests that the conflict query correctly detects overlaps and ignores cancelled bookings.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingConflictFlowIT {

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private BookingUserRepository bookingUserRepository;
    @Autowired private EquipmentRentalRepository equipmentRentalRepository;

    private static final Long FACILITY_A = 10L;
    private static final Long FACILITY_B = 20L;

    @BeforeEach
    void cleanUp() {
        bookingUserRepository.deleteAll();
        equipmentRentalRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    private BookingResponseDTO createBooking(Long facilityId, LocalDateTime start, LocalDateTime end) {
        return restTemplate.postForObject("/api/bookings",
                BookingRequestDTO.builder()
                        .userId(1L).facilityId(facilityId)
                        .startTime(start).endTime(end)
                        .totalPrice(new BigDecimal("100.00"))
                        .status(Booking.BookingStatus.PENDING)
                        .build(),
                BookingResponseDTO.class);
    }

    private List<BookingResponseDTO> getConflicting(Long facilityId, LocalDateTime start, LocalDateTime end) {
        String url = UriComponentsBuilder.fromPath("/api/bookings/facility/{fid}/conflicting")
                .queryParam("start", start.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .queryParam("end", end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .buildAndExpand(facilityId).toUriString();

        ResponseEntity<List<BookingResponseDTO>> resp = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<BookingResponseDTO>>() {});
        return resp.getBody();
    }

    @Test
    void twoOverlappingBookings_conflictEndpointDetectsBoth() {
        LocalDateTime base = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0).withSecond(0).withNano(0);

        createBooking(FACILITY_A, base, base.plusHours(2));            // 10:00–12:00
        createBooking(FACILITY_A, base.plusHours(1), base.plusHours(3)); // 11:00–13:00  ← overlaps

        List<BookingResponseDTO> conflicts = getConflicting(FACILITY_A, base, base.plusHours(3));

        assertThat(conflicts).hasSize(2);
        assertThat(bookingRepository.count()).isEqualTo(2);
    }

    @Test
    void cancellingOneBooking_removesItFromConflictResults() {
        LocalDateTime base = LocalDateTime.now().plusDays(8).withHour(14).withMinute(0).withSecond(0).withNano(0);

        BookingResponseDTO first  = createBooking(FACILITY_A, base, base.plusHours(2));
        BookingResponseDTO second = createBooking(FACILITY_A, base.plusHours(1), base.plusHours(3));

        // Cancel first booking — cancelled status is excluded from conflict query
        BookingRequestDTO cancel = BookingRequestDTO.builder()
                .userId(1L).facilityId(FACILITY_A)
                .startTime(base).endTime(base.plusHours(2))
                .totalPrice(new BigDecimal("100.00"))
                .status(Booking.BookingStatus.CANCELLED)
                .build();
        restTemplate.exchange("/api/bookings/" + first.getId(),
                HttpMethod.PUT, new HttpEntity<>(cancel), BookingResponseDTO.class);

        List<BookingResponseDTO> conflicts = getConflicting(FACILITY_A, base, base.plusHours(3));

        assertThat(conflicts).hasSize(1);
        assertThat(conflicts.get(0).getId()).isEqualTo(second.getId());
    }

    @Test
    void recurringCreation_shouldFailWithBadRequest_whenFirstOccurrenceConflicts() {
        LocalDateTime base = LocalDateTime.now().plusDays(9).withHour(10).withMinute(0).withSecond(0).withNano(0);

        // Book slot manually (PENDING)
        createBooking(FACILITY_A, base, base.plusHours(2));

        // Try to create recurring for same slot → conflict on first occurrence
        String url = UriComponentsBuilder.fromPath("/api/bookings/recurring")
                .queryParam("pattern", "WEEKLY").queryParam("occurrences", "3")
                .toUriString();

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(BookingRequestDTO.builder()
                        .userId(1L).facilityId(FACILITY_A)
                        .startTime(base).endTime(base.plusHours(2))
                        .totalPrice(new BigDecimal("100.00")).build()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Only original booking exists, no recurring ones were created
        assertThat(bookingRepository.count()).isEqualTo(1);
    }

    @Test
    void bookingsOnDifferentFacilities_shouldNeverConflict() {
        LocalDateTime base = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);

        createBooking(FACILITY_A, base, base.plusHours(2));
        createBooking(FACILITY_B, base, base.plusHours(2)); // same slot, different facility

        List<BookingResponseDTO> conflictsA = getConflicting(FACILITY_A, base, base.plusHours(2));
        List<BookingResponseDTO> conflictsB = getConflicting(FACILITY_B, base, base.plusHours(2));

        assertThat(conflictsA).hasSize(1);
        assertThat(conflictsB).hasSize(1);
        assertThat(conflictsA.get(0).getFacilityId()).isEqualTo(FACILITY_A);
        assertThat(conflictsB.get(0).getFacilityId()).isEqualTo(FACILITY_B);
    }
}
