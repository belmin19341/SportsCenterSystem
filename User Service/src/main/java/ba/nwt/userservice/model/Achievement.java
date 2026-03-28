package ba.nwt.userservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "achievement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "badge_icon", length = 50)
    private String badgeIcon;

    @Enumerated(EnumType.STRING)
    private AchievementCategory category;

    @Column(name = "unlock_criteria_value")
    private Integer unlockCriteriaValue;

    @Column(name = "unlock_criteria_type", length = 50)
    private String unlockCriteriaType;

    // ── Enum ──
    public enum AchievementCategory {
        BOOKING, RENTAL, SEASONAL, SOCIAL, MILESTONE
    }
}

