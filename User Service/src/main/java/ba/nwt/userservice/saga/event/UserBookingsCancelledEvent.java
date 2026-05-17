package ba.nwt.userservice.saga.event;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Published by Booking Service when all active bookings for the user are cancelled.
 * Consumed by User Service to finalize user deletion (DELETED — final state).
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserBookingsCancelledEvent {
    private String sagaId;
    private Long userId;
    private int cancelledCount;
    private LocalDateTime timestamp;
}
