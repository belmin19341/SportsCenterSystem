package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.dto.BookingResponseDTO;
import ba.nwt.bookingservice.exception.ResourceNotFoundException;
import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.saga.BookingSagaPublisher;
import ba.nwt.bookingservice.saga.event.BookingCreatedEvent;
import ba.nwt.bookingservice.saga.event.PaymentCompletedEvent;
import ba.nwt.bookingservice.saga.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrates the Booking side of the Booking-Payment Saga.
 *
 * Saga steps owned by this service:
 *  1. initiate()         — save Booking(PENDING)  [local transaction 1]
 *                          → publish BookingCreatedEvent
 *  2. confirmBooking()   — update Booking(CONFIRMED) [final state]
 *  3. cancelBooking()    — update Booking(CANCELLED)  [compensating / inverse action]
 */
@Service
@RequiredArgsConstructor
public class BookingSagaService {

    private static final Logger log = LoggerFactory.getLogger(BookingSagaService.class);

    private final BookingRepository bookingRepository;
    private final BookingSagaPublisher publisher;
    private final ModelMapper modelMapper;

    /**
     * Local transaction 1: persist booking as PENDING, then publish the event.
     * The HTTP response returns immediately (202 Accepted) — confirmation arrives later via RabbitMQ.
     */
    @Transactional
    public BookingResponseDTO initiate(ba.nwt.bookingservice.dto.BookingRequestDTO dto,
                                       String paymentMethod,
                                       boolean simulateFailure) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        String sagaId = UUID.randomUUID().toString();

        // ── Local Transaction 1: save booking PENDING ──────────────────────────
        Booking booking = bookingRepository.save(Booking.builder()
                .userId(dto.getUserId())
                .facilityId(dto.getFacilityId())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .totalPrice(dto.getTotalPrice())
                .isRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false)
                .recurringPattern(dto.getRecurringPattern())
                .status(Booking.BookingStatus.PENDING)
                .build());

        log.info("[SAGA][{}] Local txn 1 complete — Booking saved id={} status=PENDING", sagaId, booking.getId());

        // ── Publish event to RabbitMQ (triggers Payment Service) ──────────────
        publisher.publishBookingCreated(BookingCreatedEvent.builder()
                .sagaId(sagaId)
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .facilityId(booking.getFacilityId())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .totalPrice(booking.getTotalPrice())
                .paymentMethod(paymentMethod == null ? "CREDIT_CARD" : paymentMethod)
                .simulateFailure(simulateFailure)
                .timestamp(LocalDateTime.now())
                .build());

        return modelMapper.map(booking, BookingResponseDTO.class);
    }

    /**
     * Triggered by PaymentCompletedEvent — marks booking as CONFIRMED (final state).
     * Both local transactions have succeeded; the saga is complete.
     */
    @Transactional
    public void confirmBooking(PaymentCompletedEvent event) {
        Booking booking = bookingRepository.findById(event.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found id=" + event.getBookingId()));

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            log.warn("[SAGA][{}] confirmBooking skipped — booking {} already in status {}",
                    event.getSagaId(), event.getBookingId(), booking.getStatus());
            return;
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        log.info("[SAGA][{}] Booking id={} CONFIRMED — saga COMPLETE (paymentId={}, txn={})",
                event.getSagaId(), event.getBookingId(), event.getPaymentId(), event.getTransactionId());
    }

    /**
     * Triggered by PaymentFailedEvent — compensating (inverse) action.
     * Cancels the booking that was created in local transaction 1.
     */
    @Transactional
    public void cancelBooking(PaymentFailedEvent event) {
        Booking booking = bookingRepository.findById(event.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found id=" + event.getBookingId()));

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            log.warn("[SAGA][{}] cancelBooking skipped — booking {} already in status {}",
                    event.getSagaId(), event.getBookingId(), booking.getStatus());
            return;
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.warn("[SAGA][{}] Booking id={} CANCELLED (compensating txn) — reason: {}",
                event.getSagaId(), event.getBookingId(), event.getReason());
    }
}
