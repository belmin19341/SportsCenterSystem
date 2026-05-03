package ba.nwt.userservice.service;

import ba.nwt.userservice.dto.UserLoyaltyResponseDTO;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.Achievement;
import ba.nwt.userservice.model.UserAchievement;
import ba.nwt.userservice.model.UserLoyalty;
import ba.nwt.userservice.repository.AchievementRepository;
import ba.nwt.userservice.repository.UserAchievementRepository;
import ba.nwt.userservice.repository.UserLoyaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserLoyaltyService {

    private final UserLoyaltyRepository userLoyaltyRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final AchievementRepository achievementRepository;

    public UserLoyaltyResponseDTO getByUserId(Long userId) {
        UserLoyalty loyalty = userLoyaltyRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty record not found for user id: " + userId));
        return toResponseDTO(loyalty);
    }

    /**
     * Adds points to a user's loyalty record. If the addition triggers a tier upgrade,
     * a matching MILESTONE achievement is granted in the same transaction. Either both
     * persist or neither does.
     */
    @Transactional
    public UserLoyaltyResponseDTO addPoints(Long userId, int points) {
        if (points <= 0) {
            throw new IllegalArgumentException("Points must be positive");
        }
        UserLoyalty loyalty = userLoyaltyRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty record not found for user id: " + userId));

        UserLoyalty.LoyaltyTier oldTier = loyalty.getTier();
        loyalty.setTotalPoints(loyalty.getTotalPoints() + points);

        UserLoyalty.LoyaltyTier newTier = computeTier(loyalty.getTotalPoints());
        if (newTier != oldTier) {
            loyalty.setTier(newTier);
            loyalty.setTierAchievedAt(LocalDateTime.now());
            grantTierAchievement(loyalty.getUser().getId(), newTier);
        }
        loyalty.setUpdatedAt(LocalDateTime.now());

        UserLoyalty saved = userLoyaltyRepository.save(loyalty);
        return toResponseDTO(saved);
    }

    private UserLoyalty.LoyaltyTier computeTier(int totalPoints) {
        if (totalPoints >= 2000) return UserLoyalty.LoyaltyTier.GOLD;
        if (totalPoints >= 500)  return UserLoyalty.LoyaltyTier.SILVER;
        return UserLoyalty.LoyaltyTier.BRONZE;
    }

    private void grantTierAchievement(Long userId, UserLoyalty.LoyaltyTier tier) {
        String name = "Tier reached: " + tier.name();
        List<Achievement> matches = achievementRepository.findAll().stream()
                .filter(a -> name.equalsIgnoreCase(a.getName()))
                .toList();
        Achievement achievement;
        if (matches.isEmpty()) {
            achievement = achievementRepository.save(Achievement.builder()
                    .name(name)
                    .description("Awarded automatically upon reaching " + tier + " loyalty tier")
                    .category(Achievement.AchievementCategory.MILESTONE)
                    .badgeIcon(tier.name().toLowerCase() + "-tier.png")
                    .unlockCriteriaType("LOYALTY_TIER")
                    .unlockCriteriaValue(tier.ordinal())
                    .build());
        } else {
            achievement = matches.get(0);
        }
        userAchievementRepository.save(UserAchievement.builder()
                .user(loyaltyUserStub(userId))
                .achievement(achievement)
                .build());
    }

    private ba.nwt.userservice.model.User loyaltyUserStub(Long userId) {
        ba.nwt.userservice.model.User u = new ba.nwt.userservice.model.User();
        u.setId(userId);
        return u;
    }

    private UserLoyaltyResponseDTO toResponseDTO(UserLoyalty loyalty) {
        return UserLoyaltyResponseDTO.builder()
                .id(loyalty.getId())
                .userId(loyalty.getUser().getId())
                .username(loyalty.getUser().getUsername())
                .totalPoints(loyalty.getTotalPoints())
                .tier(loyalty.getTier())
                .tierAchievedAt(loyalty.getTierAchievedAt())
                .updatedAt(loyalty.getUpdatedAt())
                .build();
    }
}

