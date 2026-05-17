package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.saga.event.RefundCompletedEvent;
import ba.nwt.bookingservice.saga.event.RefundFailedEvent;
import ba.nwt.bookingservice.service.BookingCancellationSagaService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCancellationSagaServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock BookingCancellationSagaPublisher publisher;
    @Mock ModelMapper modelMapper;
    @InjectMocks BookingCancellationSagaService service;

    private Booking confirmedBooking;

    @BeforeEach
    void setUp() {
        confirmedBooking = Booking.builder()
                .id(10L).userId(1L).facilityId(2L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .totalPrice(new BigDecimal("150.00"))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
    }

    // ── initiate() ────────────────────────────────────────────────────────────

    @Test
    void initiate_setsBookingToCancellationPending_andPublishesEvent() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenReturn(confirmedBooking);
        when(modelMapper.map(any(), any())).thenReturn(null);

        service.initiate(10L, false);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Booking.BookingStatus.CANCELLATION_PENDING);

        verify(publisher).publishCancellationRequested(argThat(e ->
                e.getBookingId().equals(10L) && !e.isSimulateFailure()));
    }

    @Test
    void initiate_withSimulateFailure_passesFlag() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenReturn(confirmedBooking);
        when(modelMapper.map(any(), any())).thenReturn(null);

        service.initiate(10L, true);

        verify(publisher).publishCancellationRequested(argThat(e -> e.isSimulateFailure()));
    }

    @Test
    void initiate_throwsIllegalState_whenBookingNotConfirmed() {
        confirmedBooking.setStatus(Booking.BookingStatus.PENDING);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));

        assertThatThrownBy(() -> service.initiate(10L, false))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(publisher);
    }

    @Test
    void initiate_throwsResourceNotFound_whenBookingMissing() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.initiate(99L, false)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── finalizeCancel() ──────────────────────────────────────────────────────

    @Test
    void finalizeCancel_setsBookingToCancelled_finalState() {
        confirmedBooking.setStatus(Booking.BookingStatus.CANCELLATION_PENDING);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenReturn(confirmedBooking);

        service.finalizeCancel(RefundCompletedEvent.builder()
                .sagaId("s1").bookingId(10L).paymentId(5L)
                .refundedAmount(BigDecimal.TEN).timestamp(LocalDateTime.now()).build());

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Booking.BookingStatus.CANCELLED);
    }

    @Test
    void finalizeCancel_isIdempotent_whenAlreadyCancelled() {
        confirmedBooking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));

        service.finalizeCancel(RefundCompletedEvent.builder().sagaId("s").bookingId(10L).build());
        verify(bookingRepository, never()).save(any());
    }

    // ── restoreBooking() (compensating) ───────────────────────────────────────

    @Test
    void restoreBooking_compensatingAction_restoresToConfirmed() {
        confirmedBooking.setStatus(Booking.BookingStatus.CANCELLATION_PENDING);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.save(any())).thenReturn(confirmedBooking);

        service.restoreBooking(RefundFailedEvent.builder()
                .sagaId("s1").bookingId(10L).reason("no payment found").timestamp(LocalDateTime.now()).build());

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
    }

    @Test
    void restoreBooking_isIdempotent_whenAlreadyConfirmed() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(confirmedBooking));
        service.restoreBooking(RefundFailedEvent.builder().sagaId("s").bookingId(10L).reason("dup").build());
        verify(bookingRepository, never()).save(any());
    }
}
