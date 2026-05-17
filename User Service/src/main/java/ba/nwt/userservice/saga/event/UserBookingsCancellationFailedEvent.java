package ba.nwt.userservice.saga.event;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Published by Booking Service when cancellation of user's bookings fails.
 * Consumed by User Service to execute the compensating action:
 * restore user from DELETION_PENDING back to ACTIVE.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserBookingsCancellationFailedEvent {
    private String sagaId;
    private Long userId;
    private String reason;
    private LocalDateTime timestamp;
}
