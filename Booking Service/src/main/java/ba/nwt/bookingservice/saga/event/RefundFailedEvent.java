package ba.nwt.bookingservice.saga.event;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Published by Payment Service when the refund fails.
 * Consumed by Booking Service to execute the compensating action:
 * restore booking from CANCELLATION_PENDING back to CONFIRMED.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundFailedEvent {
    private String sagaId;
    private Long bookingId;
    private String reason;
    private LocalDateTime timestamp;
}
