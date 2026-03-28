package ba.nwt.resourceservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id")
    private Facility facility;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentType type;

    @Column(length = 100)
    private String category;

    @Column(name = "quantity_total", nullable = false)
    private Integer quantityTotal;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    @Column(name = "price_per_day", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_condition", columnDefinition = "VARCHAR(10) DEFAULT 'NEW'")
    private EquipmentCondition equipmentCondition = EquipmentCondition.NEW;

    @Column(name = "deposit_required", precision = 10, scale = 2)
    private BigDecimal depositRequired = BigDecimal.ZERO;

    @Column(name = "rental_count", columnDefinition = "INT DEFAULT 0")
    private Integer rentalCount = 0;

    @Column(name = "last_maintenance")
    private LocalDate lastMaintenance;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Enums ──
    public enum EquipmentType {
        RACKET, BALL, FITNESS, SKI, BICYCLE, OTHER
    }

    public enum EquipmentCondition {
        NEW, GOOD, FAIR, POOR
    }
}

