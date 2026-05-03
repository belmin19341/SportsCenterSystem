package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.config.JsonPatchUtil;
import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.model.BookingUser;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.repository.BookingUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceZ4Test {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingUserRepository bookingUserRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private JsonPatchUtil jsonPatchUtil;

    @InjectMocks private BookingService bookingService;

    private BookingRequestDTO baseRequest() {
        return BookingRequestDTO.builder()
                .userId(1L).facilityId(2L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .totalPrice(new BigDecimal("100.00"))
                .build();
    }

    @BeforeEach
    void initMapper() {
        org.mockito.Mockito.lenient()
                .when(modelMapper.map(any(Booking.class), eq(BookingResponseDTO.class)))
                .thenReturn(new BookingResponseDTO());
    }

    @Test
    void createGroup_persistsBookingAndAllParticipants_inOneTransaction() {
        lenient().when(bookingRepository.findConflicting(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        Booking saved = Booking.builder().id(10L)
                .userId(1L).facilityId(2L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                .totalPrice(new BigDecimal("100.00"))
                .status(Booking.BookingStatus.PENDING)
                .build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        bookingService.createGroup(baseRequest(), List.of(1L, 2L, 3L, 4L));

        verify(bookingRepository, times(1)).save(any(Booking.class));
        ArgumentCaptor<BookingUser> participantCaptor = ArgumentCaptor.forClass(BookingUser.class);
        verify(bookingUserRepository, times(4)).save(participantCaptor.capture());

        // 100.00 / 4 = 25.00 per participant
        assertThat(participantCaptor.getAllValues())
                .allSatisfy(p -> {
                    assertThat(p.getAmountDue()).isEqualByComparingTo("25.00");
                    assertThat(p.getPaymentStatus()).isEqualTo(BookingUser.PaymentStatus.PENDING);
                    assertThat(p.getBooking()).isSameAs(saved);
                });
    }

    @Test
    void createGroup_emptyParticipants_throws400() {
        assertThatThrownBy(() -> bookingService.createGroup(baseRequest(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one participant");
        verifyNoInteractions(bookingRepository, bookingUserRepository);
    }

    @Test
    void createGroup_conflictingSlot_throws_andDoesNotPersist() {
        Booking conflict = Booking.builder().id(99L).build();
        when(bookingRepository.findConflicting(any(), any(), any()))
                .thenReturn(List.of(conflict));

        assertThatThrownBy(() -> bookingService.createGroup(baseRequest(), List.of(1L, 2L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conflicts with booking id=99");

        verify(bookingRepository, never()).save(any(Booking.class));
        verifyNoInteractions(bookingUserRepository);
    }

    @Test
    void createRecurring_savesAllOccurrencesInOneCall() {
        lenient().when(bookingRepository.findConflicting(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        bookingService.createRecurring(baseRequest(), "WEEKLY", 4);

        ArgumentCaptor<List<Booking>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookingRepository, times(1)).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(4);
        assertThat(captor.getValue()).allSatisfy(b -> {
            assertThat(b.getIsRecurring()).isTrue();
            assertThat(b.getRecurringPattern()).isEqualTo("WEEKLY");
            assertThat(b.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);
        });
    }

    @Test
    void createRecurring_invalidOccurrences_throws() {
        assertThatThrownBy(() -> bookingService.createRecurring(baseRequest(), "WEEKLY", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bookingService.createRecurring(baseRequest(), "WEEKLY", 53))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bookingService.createRecurring(baseRequest(), "BOGUS", 2))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
