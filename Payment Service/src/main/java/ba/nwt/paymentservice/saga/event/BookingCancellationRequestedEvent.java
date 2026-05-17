package ba.nwt.paymentservice.saga.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingCancellationRequestedEvent {
    private String sagaId;
    private Long bookingId;
    private Long userId;
    private BigDecimal refundAmount;
    private LocalDateTime timestamp;
    private boolean simulateFailure;
}
