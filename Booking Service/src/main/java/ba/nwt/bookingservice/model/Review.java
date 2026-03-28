package ba.nwt.bookingservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;  // ref. User Service

    @Column(name = "reviewed_entity_id", nullable = false)
    private Long reviewedEntityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reviewed_entity_type", nullable = false)
    private ReviewedEntityType reviewedEntityType;

    @Column(nullable = false)
    private Integer rating;  // 1-5

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Enum ──
    public enum ReviewedEntityType {
        FACILITY, EQUIPMENT
    }
}

