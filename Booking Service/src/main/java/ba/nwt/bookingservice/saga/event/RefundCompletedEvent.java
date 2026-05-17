package ba.nwt.bookingservice.saga.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published by Payment Service when the refund succeeds.
 * Consumed by Booking Service to finalize cancellation (CANCELLED — final state).
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundCompletedEvent {
    private String sagaId;
    private Long bookingId;
    private Long paymentId;
    private BigDecimal refundedAmount;
    private LocalDateTime timestamp;
}
