package ba.nwt.bookingservice.saga.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RentalPaymentCompletedEvent {
    private String sagaId;
    private Long rentalId;
    private Long paymentId;
    private String transactionId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
