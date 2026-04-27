package ba.nwt.userservice.service;

import ba.nwt.userservice.dto.UserLoyaltyResponseDTO;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.Achievement;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.model.UserAchievement;
import ba.nwt.userservice.model.UserLoyalty;
import ba.nwt.userservice.repository.AchievementRepository;
import ba.nwt.userservice.repository.UserAchievementRepository;
import ba.nwt.userservice.repository.UserLoyaltyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLoyaltyServiceTest {

    @Mock private UserLoyaltyRepository loyaltyRepo;
    @Mock private UserAchievementRepository userAchievementRepo;
    @Mock private AchievementRepository achievementRepo;

    @InjectMocks private UserLoyaltyService service;

    private User user;
    private UserLoyalty loyalty;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").build();
        loyalty = UserLoyalty.builder()
                .id(10L).user(user).totalPoints(100).tier(UserLoyalty.LoyaltyTier.BRONZE).build();
    }

    @Test
    void addPoints_belowThreshold_doesNotGrantAchievement() {
        when(loyaltyRepo.findByUserId(1L)).thenReturn(Optional.of(loyalty));
        when(loyaltyRepo.save(any(UserLoyalty.class))).thenAnswer(inv -> inv.getArgument(0));

        UserLoyaltyResponseDTO out = service.addPoints(1L, 50);

        assertThat(out.getTotalPoints()).isEqualTo(150);
        assertThat(out.getTier()).isEqualTo(UserLoyalty.LoyaltyTier.BRONZE);
        verify(userAchievementRepo, never()).save(any());
        verify(achievementRepo, never()).save(any());
    }

    @Test
    void addPoints_crossingTierBoundary_grantsAchievementInSameTransaction() {
        // 100 + 500 = 600 → SILVER
        when(loyaltyRepo.findByUserId(1L)).thenReturn(Optional.of(loyalty));
        when(loyaltyRepo.save(any(UserLoyalty.class))).thenAnswer(inv -> inv.getArgument(0));
        when(achievementRepo.findAll()).thenReturn(Collections.emptyList());
        when(achievementRepo.save(any(Achievement.class))).thenAnswer(inv -> {
            Achievement a = inv.getArgument(0); a.setId(99L); return a;
        });

        UserLoyaltyResponseDTO out = service.addPoints(1L, 500);

        assertThat(out.getTotalPoints()).isEqualTo(600);
        assertThat(out.getTier()).isEqualTo(UserLoyalty.LoyaltyTier.SILVER);

        ArgumentCaptor<UserAchievement> captor = ArgumentCaptor.forClass(UserAchievement.class);
        verify(userAchievementRepo).save(captor.capture());
        assertThat(captor.getValue().getAchievement().getId()).isEqualTo(99L);
        assertThat(captor.getValue().getUser().getId()).isEqualTo(1L);
    }

    @Test
    void addPoints_invalidPoints_throws() {
        assertThatThrownBy(() -> service.addPoints(1L, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addPoints_userMissing_throwsNotFound() {
        when(loyaltyRepo.findByUserId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addPoints(99L, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
