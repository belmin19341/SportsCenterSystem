package ba.nwt.paymentservice.saga.event;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Published by Payment Service when payment processing fails.
 * Consumed by Booking Service to execute the compensating action (cancel the booking).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEvent {

    private String sagaId;

    private Long bookingId;

    /** Human-readable failure reason. */
    private String reason;

    private LocalDateTime timestamp;
}
