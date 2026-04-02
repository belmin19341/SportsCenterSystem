package ba.nwt.userservice.dto;

import ba.nwt.userservice.model.Achievement;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AchievementResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String badgeIcon;
    private Achievement.AchievementCategory category;
    private Integer unlockCriteriaValue;
    private String unlockCriteriaType;
}

