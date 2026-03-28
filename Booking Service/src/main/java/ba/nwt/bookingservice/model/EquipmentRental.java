package ba.nwt.bookingservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment_rental")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipmentRental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;  // ref. User Service

    @Column(name = "equipment_id", nullable = false)
    private Long equipmentId;  // ref. Resource Service

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;  // nullable — samostalni najam

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(columnDefinition = "INT DEFAULT 1")
    private Integer quantity = 1;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "deposit_paid", precision = 10, scale = 2)
    private BigDecimal depositPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_on_return")
    private ConditionOnReturn conditionOnReturn;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(15) DEFAULT 'RESERVED'")
    private RentalStatus status = RentalStatus.RESERVED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Enums ──
    public enum ConditionOnReturn {
        SAME, MINOR_DAMAGE, MAJOR_DAMAGE, LOST
    }

    public enum RentalStatus {
        RESERVED, ACTIVE, RETURNED, OVERDUE, CANCELLED
    }
}

