package ba.nwt.userservice.service;

import ba.nwt.userservice.dto.AchievementRequestDTO;
import ba.nwt.userservice.dto.AchievementResponseDTO;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.Achievement;
import ba.nwt.userservice.repository.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final ModelMapper modelMapper;

    public List<AchievementResponseDTO> getAllAchievements() {
        return achievementRepository.findAll().stream()
                .map(a -> modelMapper.map(a, AchievementResponseDTO.class))
                .collect(Collectors.toList());
    }

    public AchievementResponseDTO getAchievementById(Long id) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found with id: " + id));
        return modelMapper.map(achievement, AchievementResponseDTO.class);
    }

    public List<AchievementResponseDTO> getByCategory(Achievement.AchievementCategory category) {
        return achievementRepository.findByCategory(category).stream()
                .map(a -> modelMapper.map(a, AchievementResponseDTO.class))
                .collect(Collectors.toList());
    }

    public AchievementResponseDTO createAchievement(AchievementRequestDTO dto) {
        Achievement achievement = modelMapper.map(dto, Achievement.class);
        Achievement saved = achievementRepository.save(achievement);
        return modelMapper.map(saved, AchievementResponseDTO.class);
    }

    public AchievementResponseDTO updateAchievement(Long id, AchievementRequestDTO dto) {
        Achievement achievement = achievementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Achievement not found with id: " + id));

        achievement.setName(dto.getName());
        achievement.setDescription(dto.getDescription());
        achievement.setBadgeIcon(dto.getBadgeIcon());
        achievement.setCategory(dto.getCategory());
        achievement.setUnlockCriteriaValue(dto.getUnlockCriteriaValue());
        achievement.setUnlockCriteriaType(dto.getUnlockCriteriaType());

        Achievement saved = achievementRepository.save(achievement);
        return modelMapper.map(saved, AchievementResponseDTO.class);
    }

    public void deleteAchievement(Long id) {
        if (!achievementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Achievement not found with id: " + id);
        }
        achievementRepository.deleteById(id);
    }
}

