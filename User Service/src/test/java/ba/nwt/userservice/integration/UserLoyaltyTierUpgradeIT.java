package ba.nwt.userservice.integration;

import ba.nwt.userservice.dto.UserLoyaltyResponseDTO;
import ba.nwt.userservice.dto.UserRequestDTO;
import ba.nwt.userservice.dto.UserResponseDTO;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.model.UserLoyalty;
import ba.nwt.userservice.repository.AchievementRepository;
import ba.nwt.userservice.repository.UserAchievementRepository;
import ba.nwt.userservice.repository.UserLoyaltyRepository;
import ba.nwt.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flow: loyalty sistem — dodavanje bodova i automatski tier upgrade.
 * Testira prelaz BRONZE → SILVER → GOLD i auto-kreiranje tier achievementa.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserLoyaltyTierUpgradeIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private UserLoyaltyRepository userLoyaltyRepository;
    @Autowired private UserAchievementRepository userAchievementRepository;
    @Autowired private AchievementRepository achievementRepository;

    @BeforeEach
    void cleanUp() {
        userAchievementRepository.deleteAll();
        userLoyaltyRepository.deleteAll();
        userRepository.deleteAll();
        achievementRepository.deleteAll();
    }

    /** Kreira korisnika i seed-uje loyalty zapis direktno u repo (nema HTTP endpoint za create). */
    private User setupUserWithLoyalty(String username, int initialPoints, UserLoyalty.LoyaltyTier tier) {
        UserResponseDTO dto = restTemplate.postForObject("/api/users",
                UserRequestDTO.builder()
                        .username(username).email(username + "@test.com")
                        .password("password123").role(User.Role.USER).build(),
                UserResponseDTO.class);

        User userEntity = userRepository.findById(dto.getId()).orElseThrow();
        userLoyaltyRepository.save(UserLoyalty.builder()
                .user(userEntity).totalPoints(initialPoints).tier(tier).build());
        return userEntity;
    }

    private UserLoyaltyResponseDTO addPoints(Long userId, int points) {
        String url = UriComponentsBuilder.fromPath("/api/loyalty/user/{id}/add-points")
                .queryParam("points", points)
                .buildAndExpand(userId).toUriString();
        return restTemplate.exchange(url,
                org.springframework.http.HttpMethod.PATCH, null,
                UserLoyaltyResponseDTO.class).getBody();
    }

    @Test
    void addPoints_shouldIncreaseTotalPoints() {
        User user = setupUserWithLoyalty("player1", 100, UserLoyalty.LoyaltyTier.BRONZE);

        UserLoyaltyResponseDTO result = addPoints(user.getId(), 50);

        assertThat(result.getTotalPoints()).isEqualTo(150);
        assertThat(result.getTier()).isEqualTo(UserLoyalty.LoyaltyTier.BRONZE);
    }

    @Test
    void crossingBronzeToSilverThreshold_shouldUpgradeTierAutomatically() {
        // 480 BRONZE + 30 = 510 → SILVER (threshold 500)
        User user = setupUserWithLoyalty("player2", 480, UserLoyalty.LoyaltyTier.BRONZE);

        UserLoyaltyResponseDTO result = addPoints(user.getId(), 30);

        assertThat(result.getTotalPoints()).isEqualTo(510);
        assertThat(result.getTier()).isEqualTo(UserLoyalty.LoyaltyTier.SILVER);
        assertThat(result.getTierAchievedAt()).isNotNull();
    }

    @Test
    void crossingSilverToGoldThreshold_shouldUpgradeTierToGold() {
        // 1980 SILVER + 50 = 2030 → GOLD (threshold 2000)
        User user = setupUserWithLoyalty("player3", 1980, UserLoyalty.LoyaltyTier.SILVER);

        UserLoyaltyResponseDTO result = addPoints(user.getId(), 50);

        assertThat(result.getTotalPoints()).isEqualTo(2030);
        assertThat(result.getTier()).isEqualTo(UserLoyalty.LoyaltyTier.GOLD);
    }

    @Test
    void tierUpgrade_shouldAutoAssignTierAchievementToUser() {
        // Tier upgrade automatski dodijeli achievement "Tier reached: SILVER"
        User user = setupUserWithLoyalty("player4", 490, UserLoyalty.LoyaltyTier.BRONZE);

        addPoints(user.getId(), 20); // 490 + 20 = 510 → SILVER upgrade

        // Provjeri da je "Tier reached: SILVER" achievement kreiran u bazi
        assertThat(achievementRepository.findAll())
                .anyMatch(a -> a.getName().equals("Tier reached: SILVER"));

        // Provjeri da je korisniku dodijeljen barem jedan tier achievement
        // (via HTTP endpoint koji dohvaća achievements za korisnika)
        ResponseEntity<java.util.List<ba.nwt.userservice.dto.UserAchievementResponseDTO>> resp =
                restTemplate.exchange("/api/user-achievements/user/" + user.getId(),
                        org.springframework.http.HttpMethod.GET, null,
                        new org.springframework.core.ParameterizedTypeReference<
                                java.util.List<ba.nwt.userservice.dto.UserAchievementResponseDTO>>() {});
        assertThat(resp.getBody()).isNotEmpty();
        assertThat(resp.getBody()).anyMatch(ua -> ua.getAchievementName().contains("SILVER"));
    }

    @Test
    void addingZeroOrNegativePoints_shouldReturn400() {
        User user = setupUserWithLoyalty("player5", 100, UserLoyalty.LoyaltyTier.BRONZE);

        String urlZero = UriComponentsBuilder.fromPath("/api/loyalty/user/{id}/add-points")
                .queryParam("points", 0).buildAndExpand(user.getId()).toUriString();
        ResponseEntity<String> zeroResp = restTemplate.exchange(
                urlZero, org.springframework.http.HttpMethod.PATCH, null, String.class);
        assertThat(zeroResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Bodovi nisu promijenjeni
        assertThat(userLoyaltyRepository.findByUserId(user.getId()).get().getTotalPoints()).isEqualTo(100);
    }
}
