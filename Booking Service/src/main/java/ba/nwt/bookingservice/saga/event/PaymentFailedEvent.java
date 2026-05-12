package ba.nwt.bookingservice.saga.event;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Published by Payment Service when payment processing fails.
 * Consumed by Booking Service to execute the compensating transaction
 * (cancel the booking — inverse of the initial PENDING save).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEvent {

    private String sagaId;

    private Long bookingId;

    /** Human-readable reason for the failure. */
    private String reason;

    private LocalDateTime timestamp;
}
