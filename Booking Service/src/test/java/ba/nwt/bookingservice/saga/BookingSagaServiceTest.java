package ba.nwt.bookingservice.saga;

import ba.nwt.bookingservice.dto.BookingRequestDTO;
import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.saga.event.PaymentCompletedEvent;
import ba.nwt.bookingservice.saga.event.PaymentFailedEvent;
import ba.nwt.bookingservice.service.BookingSagaService;
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

/**
 * Unit tests for BookingSagaService — the Booking Service's side of the Saga.
 *
 * Tests cover:
 *  1. initiate()      — local transaction 1 (PENDING save) + event publishing
 *  2. confirmBooking() — happy path final state (CONFIRMED)
 *  3. cancelBooking()  — compensating transaction (CANCELLED)
 *  4. Idempotency checks (skip if already in terminal state)
 */
@ExtendWith(MockitoExtension.class)
class BookingSagaServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSagaPublisher publisher;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private BookingSagaService bookingSagaService;

    private BookingRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        validRequest = BookingRequestDTO.builder()
                .userId(1L)
                .facilityId(2L)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .totalPrice(new BigDecimal("150.00"))
                .isRecurring(false)
                .build();
    }

    // ── initiate() ────────────────────────────────────────────────────────────

    @Test
    void initiate_savesBookingAsPending_andPublishesEvent() {
        Booking saved = Booking.builder()
                .id(10L)
                .userId(1L)
                .facilityId(2L)
                .startTime(validRequest.getStartTime())
                .endTime(validRequest.getEndTime())
                .totalPrice(new BigDecimal("150.00"))
                .status(Booking.BookingStatus.PENDING)
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);
        when(modelMapper.map(saved, BookingResponseDTO.class))
                .thenReturn(BookingResponseDTO.builder().id(10L)
                        .status(Booking.BookingStatus.PENDING).build());

        BookingResponseDTO result = bookingSagaService.initiate(validRequest, "CREDIT_CARD", false);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Booking.BookingStatus.PENDING);

        // Verify booking saved with PENDING status
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertThat(bookingCaptor.getValue().getStatus()).isEqualTo(Booking.BookingStatus.PENDING);

        // Verify BookingCreatedEvent was published
        verify(publisher).publishBookingCreated(argThat(event ->
                event.getBookingId().equals(10L)
                && event.getSagaId() != null
                && !event.isSimulateFailure()
        ));
    }

    @Test
    void initiate_withSimulateFailure_publishesEventWithSimulateFlagTrue() {
        Booking saved = Booking.builder().id(11L).status(Booking.BookingStatus.PENDING).build();
        when(bookingRepository.save(any())).thenReturn(saved);
        when(modelMapper.map(any(), eq(BookingResponseDTO.class)))
                .thenReturn(BookingResponseDTO.builder().id(11L).build());

        bookingSagaService.initiate(validRequest, "PAYPAL", true);

        verify(publisher).publishBookingCreated(argThat(event -> event.isSimulateFailure()));
    }

    @Test
    void initiate_throwsIllegalArgument_whenEndTimeBeforeStartTime() {
        validRequest.setEndTime(validRequest.getStartTime().minusHours(1));

        assertThatThrownBy(() -> bookingSagaService.initiate(validRequest, "CREDIT_CARD", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End time must be after start time");

        verifyNoInteractions(publisher);
    }

    // ── confirmBooking() ──────────────────────────────────────────────────────

    @Test
    void confirmBooking_updatesStatusToConfirmed() {
        Booking booking = Booking.builder()
                .id(10L).status(Booking.BookingStatus.PENDING).build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenReturn(booking);

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .sagaId("saga-001")
                .bookingId(10L)
                .paymentId(99L)
                .transactionId("TXN-TEST")
                .amount(new BigDecimal("150.00"))
                .timestamp(LocalDateTime.now())
                .build();

        bookingSagaService.confirmBooking(event);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Booking.BookingStatus.CONFIRMED);
    }

    @Test
    void confirmBooking_isIdempotent_whenAlreadyConfirmed() {
        Booking booking = Booking.builder()
                .id(10L).status(Booking.BookingStatus.CONFIRMED).build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .sagaId("saga-dup")
                .bookingId(10L)
                .build();

        bookingSagaService.confirmBooking(event);

        // Should not save again when already in terminal state
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void confirmBooking_throwsResourceNotFoundException_whenBookingMissing() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .sagaId("saga-x")
                .bookingId(99L)
                .build();

        assertThatThrownBy(() -> bookingSagaService.confirmBooking(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── cancelBooking() (compensating transaction) ────────────────────────────

    @Test
    void cancelBooking_compensatingTransaction_updatesStatusToCancelled() {
        Booking booking = Booking.builder()
                .id(10L).status(Booking.BookingStatus.PENDING).build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenReturn(booking);

        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .sagaId("saga-002")
                .bookingId(10L)
                .reason("Card declined")
                .timestamp(LocalDateTime.now())
                .build();

        bookingSagaService.cancelBooking(event);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Booking.BookingStatus.CANCELLED);
    }

    @Test
    void cancelBooking_isIdempotent_whenAlreadyCancelled() {
        Booking booking = Booking.builder()
                .id(10L).status(Booking.BookingStatus.CANCELLED).build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .sagaId("saga-dup")
                .bookingId(10L)
                .reason("Already cancelled")
                .build();

        bookingSagaService.cancelBooking(event);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_throwsResourceNotFoundException_whenBookingMissing() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .sagaId("saga-y")
                .bookingId(999L)
                .reason("Test")
                .build();

        assertThatThrownBy(() -> bookingSagaService.cancelBooking(event))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
