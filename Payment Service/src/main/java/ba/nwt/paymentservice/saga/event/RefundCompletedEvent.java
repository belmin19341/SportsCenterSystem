package ba.nwt.paymentservice.saga.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundCompletedEvent {
    private String sagaId;
    private Long bookingId;
    private Long paymentId;
    private BigDecimal refundedAmount;
    private LocalDateTime timestamp;
}
