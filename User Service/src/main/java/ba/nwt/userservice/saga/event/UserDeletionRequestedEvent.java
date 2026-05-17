package ba.nwt.userservice.saga.event;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Published by User Service when a user deletion is initiated.
 * Consumed by Booking Service to cancel all active bookings for that user (local TX 2).
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDeletionRequestedEvent {
    private String sagaId;
    private Long userId;
    private String username;
    private LocalDateTime timestamp;
}
