package ba.nwt.bookingservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;  // ref. User Service

    @Column(name = "facility_id", nullable = false)
    private Long facilityId;  // ref. Resource Service

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "is_recurring", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isRecurring = false;

    @Column(name = "recurring_pattern", length = 100)
    private String recurringPattern;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private BookingStatus status = BookingStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Enum ──
    public enum BookingStatus {
        PENDING, CONFIRMED, COMPLETED, CANCELLED, TEMPORARY_HOLD
    }
}

