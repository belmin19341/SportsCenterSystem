package ba.nwt.bookingservice.saga.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published by Booking Service when a cancellation is requested for a CONFIRMED booking.
 * Consumed by Payment Service to trigger a refund (local transaction 2).
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingCancellationRequestedEvent {
    private String sagaId;
    private Long bookingId;
    private Long userId;
    private BigDecimal refundAmount;
    private LocalDateTime timestamp;
    private boolean simulateFailure;
}
