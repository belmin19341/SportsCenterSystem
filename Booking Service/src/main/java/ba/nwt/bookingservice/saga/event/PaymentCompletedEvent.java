package ba.nwt.bookingservice.saga.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published by Payment Service when payment is successfully processed (status = PAID).
 * Consumed by Booking Service to confirm the booking (final state).
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
