package ba.nwt.bookingservice.service;

import ba.nwt.bookingservice.model.Booking;
import ba.nwt.bookingservice.repository.BookingRepository;
import ba.nwt.bookingservice.saga.UserDeletionBookingSagaPublisher;
import ba.nwt.bookingservice.saga.event.UserBookingsCancelledEvent;
import ba.nwt.bookingservice.saga.event.UserBookingsCancellationFailedEvent;
import ba.nwt.bookingservice.saga.event.UserDeletionRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User Deletion Saga — Booking Service side.
 *
 * Local TX 2: cancel all PENDING/CONFIRMED bookings for the given userId.
 * On success → publish UserBookingsCancelledEvent → User finalized as DELETED.
 * On failure → publish UserBookingsCancellationFailedEvent → User restored to ACTIVE.
 */
@Service
@RequiredArgsConstructor
public class UserDeletionBookingSagaService {

    private static final Logger log = LoggerFactory.getLogger(UserDeletionBookingSagaService.class);

    private final BookingRepository bookingRepository;
    private final UserDeletionBookingSagaPublisher publisher;

    private static final List<Booking.BookingStatus> ACTIVE_STATUSES =
            List.of(Booking.BookingStatus.PENDING, Booking.BookingStatus.CONFIRMED, Booking.BookingStatus.CANCELLATION_PENDING);

    @Transactional
    public void cancelUserBookings(UserDeletionRequestedEvent event) {
        log.info("[SAGA-USER][{}] Local TX 2 start — cancelling bookings for userId={}", event.getSagaId(), event.getUserId());

        try {
            List<Booking> activeBookings = bookingRepository.findByUserIdAndStatusIn(event.getUserId(), ACTIVE_STATUSES);

            activeBookings.forEach(b -> b.setStatus(Booking.BookingStatus.CANCELLED));
            bookingRepository.saveAll(activeBookings);

            log.info("[SAGA-USER][{}] Cancelled {} bookings for userId={}", event.getSagaId(), activeBookings.size(), event.getUserId());

            publisher.publishBookingsCancelled(UserBookingsCancelledEvent.builder()
                    .sagaId(event.getSagaId())
                    .userId(event.getUserId())
                    .cancelledCount(activeBookings.size())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            log.error("[SAGA-USER][{}] Failed to cancel bookings for userId={}: {}", event.getSagaId(), event.getUserId(), ex.getMessage());
            publisher.publishBookingsCancellationFailed(UserBookingsCancellationFailedEvent.builder()
                    .sagaId(event.getSagaId())
                    .userId(event.getUserId())
                    .reason("Failed to cancel bookings: " + ex.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }
}
