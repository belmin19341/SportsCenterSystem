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
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flow: kreiranje recurring (ponavljajućih) rezervacija.
 * Testira tačan broj kreiranog, intervale između rezervacija,
 * isRecurring flag i prekid na konfliktu.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BookingRecurringFlowIT {

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

    private List<BookingResponseDTO> postRecurring(LocalDateTime start, LocalDateTime end,
                                                    Long facilityId, String pattern, int occurrences) {
        String url = UriComponentsBuilder.fromPath("/api/bookings/recurring")
                .queryParam("pattern", pattern)
                .queryParam("occurrences", occurrences)
                .toUriString();

        ResponseEntity<List<BookingResponseDTO>> resp = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(BookingRequestDTO.builder()
                        .userId(1L).facilityId(facilityId)
                        .startTime(start).endTime(end)
                        .totalPrice(new BigDecimal("80.00")).build()),
                new ParameterizedTypeReference<List<BookingResponseDTO>>() {});
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody();
    }

    @Test
    void createFourWeeklyBookings_allPersistedAndReturnedInResponse() {
        LocalDateTime start = LocalDateTime.now().plusDays(14).withHour(10).withMinute(0).withSecond(0).withNano(0);

        List<BookingResponseDTO> bookings = postRecurring(start, start.plusHours(2), 10L, "WEEKLY", 4);

        assertThat(bookings).hasSize(4);
        assertThat(bookingRepository.count()).isEqualTo(4);
    }

    @Test
    void weeklyRecurring_eachBookingIsExactlySevenDaysAfterPrevious() {
        LocalDateTime start = LocalDateTime.now().plusDays(15).withHour(10).withMinute(0).withSecond(0).withNano(0);

        List<BookingResponseDTO> bookings = postRecurring(start, start.plusHours(2), 11L, "WEEKLY", 4);

        for (int i = 1; i < bookings.size(); i++) {
            long daysBetween = ChronoUnit.DAYS.between(
                    bookings.get(i - 1).getStartTime(),
                    bookings.get(i).getStartTime());
            assertThat(daysBetween).isEqualTo(7);
        }
    }

    @Test
    void allRecurringBookings_haveIsRecurringFlagSetToTrue() {
        LocalDateTime start = LocalDateTime.now().plusDays(16).withHour(10).withMinute(0).withSecond(0).withNano(0);

        List<BookingResponseDTO> bookings = postRecurring(start, start.plusHours(2), 12L, "WEEKLY", 3);

        assertThat(bookings).allMatch(b -> Boolean.TRUE.equals(b.getIsRecurring()));
        bookingRepository.findAll().forEach(b ->
                assertThat(b.getIsRecurring()).isTrue());
    }

    @Test
    void dailyRecurring_shouldSpanConsecutiveDays() {
        LocalDateTime start = LocalDateTime.now().plusDays(17).withHour(9).withMinute(0).withSecond(0).withNano(0);

        List<BookingResponseDTO> bookings = postRecurring(start, start.plusHours(1), 13L, "DAILY", 5);

        assertThat(bookings).hasSize(5);
        for (int i = 1; i < bookings.size(); i++) {
            long daysBetween = ChronoUnit.DAYS.between(
                    bookings.get(i - 1).getStartTime(),
                    bookings.get(i).getStartTime());
            assertThat(daysBetween).isEqualTo(1);
        }
    }

    @Test
    void recurringCreation_atomicallyRollsBack_whenConflictOccursOnLaterOccurrence() {
        LocalDateTime base = LocalDateTime.now().plusDays(18).withHour(10).withMinute(0).withSecond(0).withNano(0);

        // Zauzmi treću sedmicu (occurrence index=2) za isti facility
        restTemplate.postForObject("/api/bookings",
                BookingRequestDTO.builder()
                        .userId(2L).facilityId(14L)
                        .startTime(base.plusWeeks(2)).endTime(base.plusWeeks(2).plusHours(2))
                        .totalPrice(new BigDecimal("100.00"))
                        .status(Booking.BookingStatus.PENDING).build(),
                BookingResponseDTO.class);

        // Pokušaj kreirati 4 weekly bookings — treća se sukobi s postojećom
        String url = UriComponentsBuilder.fromPath("/api/bookings/recurring")
                .queryParam("pattern", "WEEKLY").queryParam("occurrences", "4")
                .toUriString();
        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST,
                new HttpEntity<>(BookingRequestDTO.builder()
                        .userId(1L).facilityId(14L)
                        .startTime(base).endTime(base.plusHours(2))
                        .totalPrice(new BigDecimal("80.00")).build()),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        // Samo originalni booking ostaje — rollback sprečio kreiranje recurring
        assertThat(bookingRepository.count()).isEqualTo(1);
    }
}
