package ba.nwt.bookingservice.saga.event;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Published by Booking Service when a rental is saved as RESERVED.
 * Consumed by Payment Service to create and process the rental payment (local TX 2).
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RentalCreatedEvent {
    private String sagaId;
    private Long rentalId;
    private Long userId;
    private Long equipmentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer quantity;
    private BigDecimal totalPrice;
    private BigDecimal depositAmount;
    private String paymentMethod;
    private LocalDateTime timestamp;
    private boolean simulateFailure;
}
