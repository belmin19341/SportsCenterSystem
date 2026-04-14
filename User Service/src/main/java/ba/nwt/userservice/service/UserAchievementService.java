package ba.nwt.userservice.service;

import ba.nwt.userservice.dto.UserAchievementRequestDTO;
import ba.nwt.userservice.dto.UserAchievementResponseDTO;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.Achievement;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.model.UserAchievement;
import ba.nwt.userservice.repository.AchievementRepository;
import ba.nwt.userservice.repository.UserAchievementRepository;
import ba.nwt.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAchievementService {

    private final UserAchievementRepository userAchievementRepository;
    private final UserRepository userRepository;
    private final AchievementRepository achievementRepository;

    public List<UserAchievementResponseDTO> getByUserId(Long userId) {
        return userAchievementRepository.findByUserId(userId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public UserAchievementResponseDTO getById(Long id) {
        UserAchievement ua = userAchievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserAchievement not found with id: " + id));
        return toResponseDTO(ua);
    }

    public UserAchievementResponseDTO create(UserAchievementRequestDTO dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));
        Achievement achievement = achievementRepository.findById(dto.getAchievementId())
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found with id: " + dto.getAchievementId()));

        UserAchievement ua = UserAchievement.builder()
                .user(user)
                .achievement(achievement)
                .build();

        UserAchievement saved = userAchievementRepository.save(ua);
        return toResponseDTO(saved);
    }

    public void delete(Long id) {
        if (!userAchievementRepository.existsById(id)) {
            throw new ResourceNotFoundException("UserAchievement not found with id: " + id);
        }
        userAchievementRepository.deleteById(id);
    }

    private UserAchievementResponseDTO toResponseDTO(UserAchievement ua) {
        return UserAchievementResponseDTO.builder()
                .id(ua.getId())
                .userId(ua.getUser().getId())
                .username(ua.getUser().getUsername())
                .achievementId(ua.getAchievement().getId())
                .achievementName(ua.getAchievement().getName())
                .badgeIcon(ua.getAchievement().getBadgeIcon())
                .unlockedAt(ua.getUnlockedAt())
                .build();
    }
}

