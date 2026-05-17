package ba.nwt.paymentservice.saga.event;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RentalPaymentFailedEvent {
    private String sagaId;
    private Long rentalId;
    private String reason;
    private LocalDateTime timestamp;
}
