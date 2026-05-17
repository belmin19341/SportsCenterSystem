package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.saga.event.UserDeletionRequestedEvent;
import ba.nwt.bookingservice.service.UserDeletionBookingSagaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDeletionBookingSagaServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock UserDeletionBookingSagaPublisher publisher;
    @InjectMocks UserDeletionBookingSagaService service;

    private UserDeletionRequestedEvent buildEvent() {
        return UserDeletionRequestedEvent.builder()
                .sagaId("saga-user-01").userId(1L).username("test_user")
                .timestamp(LocalDateTime.now()).build();
    }

    private Booking buildBooking(Long id, Booking.BookingStatus status) {
        return Booking.builder().id(id).userId(1L).facilityId(2L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .totalPrice(new BigDecimal("100.00"))
                .status(status).build();
    }

    // ── cancelUserBookings() happy path ───────────────────────────────────────

    @Test
    void cancelUserBookings_cancelsAllActiveBookings_andPublishesSuccess() {
        List<Booking> active = List.of(
                buildBooking(1L, Booking.BookingStatus.PENDING),
                buildBooking(2L, Booking.BookingStatus.CONFIRMED));

        when(bookingRepository.findByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(active);
        when(bookingRepository.saveAll(anyList())).thenReturn(active);

        service.cancelUserBookings(buildEvent());

        verify(bookingRepository).saveAll(argThat(bookings ->
                ((List<Booking>) bookings).stream()
                        .allMatch(b -> b.getStatus() == Booking.BookingStatus.CANCELLED)));

        verify(publisher).publishBookingsCancelled(argThat(e ->
                e.getUserId().equals(1L) && e.getCancelledCount() == 2));
        verify(publisher, never()).publishBookingsCancellationFailed(any());
    }

    @Test
    void cancelUserBookings_withZeroBookings_publishesSuccessWithZeroCount() {
        when(bookingRepository.findByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        when(bookingRepository.saveAll(anyList())).thenReturn(List.of());

        service.cancelUserBookings(buildEvent());

        verify(publisher).publishBookingsCancelled(argThat(e -> e.getCancelledCount() == 0));
        verify(publisher, never()).publishBookingsCancellationFailed(any());
    }

    // ── cancelUserBookings() failure / compensating path ─────────────────────

    @Test
    void cancelUserBookings_onException_publishesFailedEvent() {
        when(bookingRepository.findByUserIdAndStatusIn(eq(1L), anyList()))
                .thenThrow(new RuntimeException("DB timeout"));

        service.cancelUserBookings(buildEvent());

        verify(publisher).publishBookingsCancellationFailed(argThat(e ->
                e.getUserId().equals(1L) && e.getReason().contains("DB timeout")));
        verify(publisher, never()).publishBookingsCancelled(any());
    }

    @Test
    void cancelUserBookings_sagaIdPropagatedToEvent() {
        when(bookingRepository.findByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(List.of());
        when(bookingRepository.saveAll(anyList())).thenReturn(List.of());

        service.cancelUserBookings(buildEvent());

        verify(publisher).publishBookingsCancelled(argThat(e -> "saga-user-01".equals(e.getSagaId())));
    }
}
