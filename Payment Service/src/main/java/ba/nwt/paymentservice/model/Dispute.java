package ba.nwt.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispute")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id")
    private Long bookingId;  // ref. Booking Service, nullable

    @Column(name = "rental_id")
    private Long rentalId;  // ref. Booking Service, nullable

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;  // ref. User Service

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'OPEN'")
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ── Enum ──
    public enum DisputeStatus {
        OPEN, IN_PROGRESS, RESOLVED
    }
}

