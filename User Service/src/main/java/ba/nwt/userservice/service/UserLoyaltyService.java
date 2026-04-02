package ba.nwt.userservice.service;

import ba.nwt.userservice.dto.UserLoyaltyResponseDTO;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.UserLoyalty;
import ba.nwt.userservice.repository.UserLoyaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserLoyaltyService {

    private final UserLoyaltyRepository userLoyaltyRepository;

    public UserLoyaltyResponseDTO getByUserId(Long userId) {
        UserLoyalty loyalty = userLoyaltyRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty record not found for user id: " + userId));
        return toResponseDTO(loyalty);
    }

    public UserLoyaltyResponseDTO addPoints(Long userId, int points) {
        UserLoyalty loyalty = userLoyaltyRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Loyalty record not found for user id: " + userId));

        loyalty.setTotalPoints(loyalty.getTotalPoints() + points);

        // Auto-tier upgrade logic
        if (loyalty.getTotalPoints() >= 500) {
            loyalty.setTier(UserLoyalty.LoyaltyTier.GOLD);
            loyalty.setTierAchievedAt(LocalDateTime.now());
        } else if (loyalty.getTotalPoints() >= 200) {
            loyalty.setTier(UserLoyalty.LoyaltyTier.SILVER);
            loyalty.setTierAchievedAt(LocalDateTime.now());
        }
        loyalty.setUpdatedAt(LocalDateTime.now());

        UserLoyalty saved = userLoyaltyRepository.save(loyalty);
        return toResponseDTO(saved);
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

