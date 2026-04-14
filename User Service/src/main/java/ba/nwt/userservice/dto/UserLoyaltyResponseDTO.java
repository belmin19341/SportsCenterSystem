package ba.nwt.userservice.dto;

import ba.nwt.userservice.model.UserLoyalty;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserLoyaltyResponseDTO {
    private Long id;
    private Long userId;
    private String username;
    private Integer totalPoints;
    private UserLoyalty.LoyaltyTier tier;
    private LocalDateTime tierAchievedAt;
    private LocalDateTime updatedAt;
}

