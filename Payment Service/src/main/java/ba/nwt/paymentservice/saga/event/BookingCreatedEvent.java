package ba.nwt.paymentservice.saga.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Received by Payment Service from Booking Service when a booking is saved as PENDING.
 * Triggers local transaction 2: create and process the payment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreatedEvent {

    private String sagaId;

    private Long bookingId;
    private Long userId;
    private Long facilityId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private BigDecimal totalPrice;

    /** CREDIT_CARD | DEBIT_CARD | PAYPAL */
    private String paymentMethod;

    private LocalDateTime timestamp;

    /** When true, this service will simulate a payment failure (for testing compensating transactions). */
    private boolean simulateFailure;
}
