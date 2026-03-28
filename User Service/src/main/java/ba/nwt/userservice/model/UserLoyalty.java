package ba.nwt.userservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_loyalty")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserLoyalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_points", columnDefinition = "INT DEFAULT 0")
    private Integer totalPoints = 0;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(10) DEFAULT 'BRONZE'")
    private LoyaltyTier tier = LoyaltyTier.BRONZE;

    @Column(name = "tier_achieved_at")
    private LocalDateTime tierAchievedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Enum ──
    public enum LoyaltyTier {
        BRONZE, SILVER, GOLD
    }
}

