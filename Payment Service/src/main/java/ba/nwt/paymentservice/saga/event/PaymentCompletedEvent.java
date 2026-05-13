package ba.nwt.paymentservice.saga.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published by Payment Service when payment succeeds.
 * Consumed by Booking Service to update booking status to CONFIRMED (final state).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {

    private String sagaId;

    private Long bookingId;
    private Long paymentId;

    private String transactionId;
    private BigDecimal amount;

    private LocalDateTime timestamp;
}
