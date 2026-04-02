package ba.nwt.userservice.dto;

import ba.nwt.userservice.model.Achievement;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AchievementRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    private String description;

    @Size(max = 50)
    private String badgeIcon;

    @NotNull(message = "Category is required")
    private Achievement.AchievementCategory category;

    @Min(value = 1, message = "Unlock criteria value must be at least 1")
    private Integer unlockCriteriaValue;

    @Size(max = 50)
    private String unlockCriteriaType;
}

