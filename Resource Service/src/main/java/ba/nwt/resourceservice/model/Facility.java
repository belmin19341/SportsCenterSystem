package ba.nwt.resourceservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "facility")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;  // ref. User Service — samo ID

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FacilityType type;

    private Integer capacity;

    @Column(name = "base_price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePricePerHour;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "working_hours_start", nullable = false)
    private LocalTime workingHoursStart;

    @Column(name = "working_hours_end", nullable = false)
    private LocalTime workingHoursEnd;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private FacilityStatus status = FacilityStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Enums ──
    public enum FacilityType {
        FOOTBALL_5V5, FOOTBALL_7V7, PADEL, TENNIS, TABLE_TENNIS
    }

    public enum FacilityStatus {
        ACTIVE, INACTIVE, MAINTENANCE
    }
}

