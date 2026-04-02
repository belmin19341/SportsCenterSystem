package ba.nwt.userservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAchievementResponseDTO {
    private Long id;
    private Long userId;
    private String username;
    private Long achievementId;
    private String achievementName;
    private String badgeIcon;
    private LocalDateTime unlockedAt;
}

