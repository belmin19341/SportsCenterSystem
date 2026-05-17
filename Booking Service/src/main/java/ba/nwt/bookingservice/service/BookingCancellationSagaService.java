package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.saga.BookingCancellationSagaPublisher;
import ba.nwt.bookingservice.saga.event.BookingCancellationRequestedEvent;
import ba.nwt.bookingservice.saga.event.RefundCompletedEvent;
import ba.nwt.bookingservice.saga.event.RefundFailedEvent;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Booking Cancellation Saga — Booking Service side.
 *
 * Steps:
 *  1. initiate()       — Booking CONFIRMED → CANCELLATION_PENDING  [local TX 1]
 *                        → publish BookingCancellationRequestedEvent
 *  2. finalizeCancel() — Booking CANCELLATION_PENDING → CANCELLED   [final state]
 *  3. restoreBooking() — Booking CANCELLATION_PENDING → CONFIRMED   [compensating — inverse of TX1]
 */
@Service
@RequiredArgsConstructor
public class BookingCancellationSagaService {

    private static final Logger log = LoggerFactory.getLogger(BookingCancellationSagaService.class);

    private final BookingRepository bookingRepository;
    private final BookingCancellationSagaPublisher publisher;
    private final ModelMapper modelMapper;

    /** Local TX 1: mark booking as CANCELLATION_PENDING and trigger refund. */
    @Transactional
    public BookingResponseDTO initiate(Long bookingId, boolean simulateFailure) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + bookingId));

        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException(
                    "Only CONFIRMED bookings can be cancelled via saga (current: " + booking.getStatus() + ")");
        }

        String sagaId = UUID.randomUUID().toString();
        booking.setStatus(Booking.BookingStatus.CANCELLATION_PENDING);
        bookingRepository.save(booking);
        log.info("[SAGA-CANCEL][{}] Local TX 1 — Booking id={} → CANCELLATION_PENDING", sagaId, bookingId);

        publisher.publishCancellationRequested(BookingCancellationRequestedEvent.builder()
                .sagaId(sagaId)
                .bookingId(bookingId)
                .userId(booking.getUserId())
                .refundAmount(booking.getTotalPrice())
                .simulateFailure(simulateFailure)
                .timestamp(LocalDateTime.now())
                .build());

        return modelMapper.map(booking, BookingResponseDTO.class);
    }

    /** Triggered by RefundCompletedEvent — booking is officially CANCELLED (final state). */
    @Transactional
    public void finalizeCancel(RefundCompletedEvent event) {
        Booking booking = bookingRepository.findById(event.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + event.getBookingId()));

        if (booking.getStatus() != Booking.BookingStatus.CANCELLATION_PENDING) {
            log.warn("[SAGA-CANCEL][{}] finalizeCancel skipped — booking {} already in {}", event.getSagaId(), event.getBookingId(), booking.getStatus());
            return;
        }
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("[SAGA-CANCEL][{}] Booking id={} CANCELLED — saga COMPLETE (refund paymentId={})", event.getSagaId(), event.getBookingId(), event.getPaymentId());
    }

    /**
     * Triggered by RefundFailedEvent — compensating (inverse) action.
     * Restores booking from CANCELLATION_PENDING back to CONFIRMED.
     */
    @Transactional
    public void restoreBooking(RefundFailedEvent event) {
        Booking booking = bookingRepository.findById(event.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found id=" + event.getBookingId()));

        if (booking.getStatus() != Booking.BookingStatus.CANCELLATION_PENDING) {
            log.warn("[SAGA-CANCEL][{}] restoreBooking skipped — booking {} already in {}", event.getSagaId(), event.getBookingId(), booking.getStatus());
            return;
        }
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        log.warn("[SAGA-CANCEL][{}] Booking id={} RESTORED to CONFIRMED (compensating TX) — reason: {}", event.getSagaId(), event.getBookingId(), event.getReason());
    }
}
