package ba.nwt.userservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserAchievementRequestDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Achievement ID is required")
    private Long achievementId;
}

