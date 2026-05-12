package ba.nwt.bookingservice.saga.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Published by Booking Service when a new booking is saved with status PENDING.
 * Consumed by Payment Service to trigger payment processing (local transaction 2).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreatedEvent {

    /** Unique identifier that links all events belonging to this saga instance. */
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

    /**
     * When true, Payment Service will intentionally fail the payment.
     * Used to test the compensating transaction path without modifying business logic.
     */
    private boolean simulateFailure;
}
