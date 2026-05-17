package ba.nwt.paymentservice.saga.event;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundFailedEvent {
    private String sagaId;
    private Long bookingId;
    private String reason;
    private LocalDateTime timestamp;
}
