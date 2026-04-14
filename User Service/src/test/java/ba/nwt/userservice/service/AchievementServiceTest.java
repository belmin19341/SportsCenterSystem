package ba.nwt.userservice.service;

import ba.nwt.userservice.dto.AchievementRequestDTO;
import ba.nwt.userservice.dto.AchievementResponseDTO;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.Achievement;
import ba.nwt.userservice.repository.AchievementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AchievementService achievementService;

    private Achievement achievement;
    private AchievementResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        achievement = Achievement.builder()
                .id(1L)
                .name("Prva rezervacija")
                .description("Napravi svoju prvu rezervaciju")
                .badgeIcon("🏅")
                .category(Achievement.AchievementCategory.BOOKING)
                .unlockCriteriaType("BOOKING_COUNT")
                .unlockCriteriaValue(1)
                .build();

        responseDTO = AchievementResponseDTO.builder()
                .id(1L)
                .name("Prva rezervacija")
                .category(Achievement.AchievementCategory.BOOKING)
                .build();
    }

    @Test
    void getAllAchievements_shouldReturnList() {
        when(achievementRepository.findAll()).thenReturn(List.of(achievement));
        when(modelMapper.map(any(Achievement.class), eq(AchievementResponseDTO.class))).thenReturn(responseDTO);

        List<AchievementResponseDTO> result = achievementService.getAllAchievements();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Prva rezervacija");
    }

    @Test
    void getById_shouldReturnAchievement() {
        when(achievementRepository.findById(1L)).thenReturn(Optional.of(achievement));
        when(modelMapper.map(achievement, AchievementResponseDTO.class)).thenReturn(responseDTO);

        AchievementResponseDTO result = achievementService.getAchievementById(1L);

        assertThat(result.getName()).isEqualTo("Prva rezervacija");
    }

    @Test
    void getById_shouldThrowNotFound() {
        when(achievementRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> achievementService.getAchievementById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteAchievement_shouldDeleteExisting() {
        when(achievementRepository.existsById(1L)).thenReturn(true);

        achievementService.deleteAchievement(1L);

        verify(achievementRepository).deleteById(1L);
    }
}

