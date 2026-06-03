package ba.nwt.userservice.integration;

import ba.nwt.userservice.dto.AchievementRequestDTO;
import ba.nwt.userservice.dto.AchievementResponseDTO;
import ba.nwt.userservice.dto.UserAchievementRequestDTO;
import ba.nwt.userservice.dto.UserAchievementResponseDTO;
import ba.nwt.userservice.dto.UserRequestDTO;
import ba.nwt.userservice.dto.UserResponseDTO;
import ba.nwt.userservice.model.Achievement;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.repository.AchievementRepository;
import ba.nwt.userservice.repository.UserAchievementRepository;
import ba.nwt.userservice.repository.UserLoyaltyRepository;
import ba.nwt.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flow: assigning achievements to users.
 * Tests the full flow: user + achievement creation,
 * assignment, visibility in profile, and deletion.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserAchievementFlowIT {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private UserAchievementRepository userAchievementRepository;
    @Autowired private UserLoyaltyRepository userLoyaltyRepository;
    @Autowired private AchievementRepository achievementRepository;

    @BeforeEach
    void cleanUp() {
        userAchievementRepository.deleteAll();
        userLoyaltyRepository.deleteAll();
        userRepository.deleteAll();
        achievementRepository.deleteAll();
    }

    private UserResponseDTO createUser(String username, String email) {
        return restTemplate.postForObject("/api/users",
                UserRequestDTO.builder()
                        .username(username).email(email)
                        .password("password123").role(User.Role.USER).build(),
                UserResponseDTO.class);
    }

    private AchievementResponseDTO createAchievement(String name) {
        return restTemplate.postForObject("/api/achievements",
                AchievementRequestDTO.builder()
                        .name(name)
                        .category(Achievement.AchievementCategory.MILESTONE)
                        .build(),
                AchievementResponseDTO.class);
    }

    private UserAchievementResponseDTO assignAchievement(Long userId, Long achievementId) {
        return restTemplate.postForObject("/api/user-achievements",
                UserAchievementRequestDTO.builder()
                        .userId(userId).achievementId(achievementId).build(),
                UserAchievementResponseDTO.class);
    }

    private List<UserAchievementResponseDTO> getAchievementsForUser(Long userId) {
        ResponseEntity<List<UserAchievementResponseDTO>> resp = restTemplate.exchange(
                "/api/user-achievements/user/" + userId, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<UserAchievementResponseDTO>>() {});
        return resp.getBody();
    }

    @Test
    void assignAchievement_shouldAppearInUsersAchievementList() {
        UserResponseDTO user = createUser("ivan", "ivan@example.com");
        AchievementResponseDTO ach = createAchievement("First Booking");

        assignAchievement(user.getId(), ach.getId());

        List<UserAchievementResponseDTO> achievements = getAchievementsForUser(user.getId());

        assertThat(achievements).hasSize(1);
        assertThat(achievements.get(0).getUserId()).isEqualTo(user.getId());
        assertThat(achievements.get(0).getAchievementId()).isEqualTo(ach.getId());
        assertThat(achievements.get(0).getAchievementName()).isEqualTo("First Booking");
        assertThat(achievements.get(0).getUnlockedAt()).isNotNull();
    }

    @Test
    void assignMultipleAchievements_allVisibleForUser() {
        UserResponseDTO user = createUser("marko", "marko@example.com");
        AchievementResponseDTO ach1 = createAchievement("10 Bookings");
        AchievementResponseDTO ach2 = createAchievement("Regular Player");
        AchievementResponseDTO ach3 = createAchievement("Gear Up");

        assignAchievement(user.getId(), ach1.getId());
        assignAchievement(user.getId(), ach2.getId());
        assignAchievement(user.getId(), ach3.getId());

        List<UserAchievementResponseDTO> achievements = getAchievementsForUser(user.getId());

        assertThat(achievements).hasSize(3);
        assertThat(achievements).extracting(UserAchievementResponseDTO::getAchievementName)
                .containsExactlyInAnyOrder("10 Bookings", "Regular Player", "Gear Up");
    }

    @Test
    void deleteAchievementAssignment_shouldDisappearFromUserProfile() {
        UserResponseDTO user = createUser("ana", "ana@example.com");
        AchievementResponseDTO ach = createAchievement("Bonus achievement");

        UserAchievementResponseDTO assignment = assignAchievement(user.getId(), ach.getId());
        assertThat(getAchievementsForUser(user.getId())).hasSize(1);

        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                "/api/user-achievements/" + assignment.getId(), HttpMethod.DELETE, null, Void.class);
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(getAchievementsForUser(user.getId())).isEmpty();
        assertThat(userAchievementRepository.count()).isZero();
    }

    @Test
    void achievementsArePerUser_twoUsersHaveSeparateAchievements() {
        UserResponseDTO user1 = createUser("petra", "petra@example.com");
        UserResponseDTO user2 = createUser("luka",  "luka@example.com");
        AchievementResponseDTO ach = createAchievement("Shared achievement");

        assignAchievement(user1.getId(), ach.getId());
        assignAchievement(user2.getId(), ach.getId());

        assertThat(getAchievementsForUser(user1.getId())).hasSize(1);
        assertThat(getAchievementsForUser(user2.getId())).hasSize(1);
        // Each user has their own assignment instance
        assertThat(userAchievementRepository.count()).isEqualTo(2);
    }

    @Test
    void assignToNonexistentUser_shouldReturn404() {
        AchievementResponseDTO ach = createAchievement("Phantom achievement");

        ResponseEntity<String> resp = restTemplate.postForEntity("/api/user-achievements",
                UserAchievementRequestDTO.builder().userId(99999L).achievementId(ach.getId()).build(),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(userAchievementRepository.count()).isZero();
    }
}
