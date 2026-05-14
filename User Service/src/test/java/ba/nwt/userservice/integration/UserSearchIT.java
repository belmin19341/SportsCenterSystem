package ba.nwt.userservice.integration;

import ba.nwt.userservice.dto.UserRequestDTO;
import ba.nwt.userservice.dto.UserResponseDTO;
import ba.nwt.userservice.model.User;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for User paginated search endpoint.
 * Validates filtering by role and keyword, and pagination behaviour.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserSearchIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserAchievementRepository userAchievementRepository;

    @Autowired
    private UserLoyaltyRepository userLoyaltyRepository;

    @BeforeEach
    void cleanUp() {
        userAchievementRepository.deleteAll();
        userLoyaltyRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void createUser(String username, String email, User.Role role) {
        restTemplate.postForObject("/api/users",
                UserRequestDTO.builder()
                        .username(username)
                        .email(email)
                        .password("password123")
                        .role(role)
                        .build(),
                UserResponseDTO.class);
    }

    @Test
    void searchByRole_shouldReturnOnlyUsersWithMatchingRole() {
        createUser("user1", "user1@example.com", User.Role.USER);
        createUser("user2", "user2@example.com", User.Role.USER);
        createUser("owner1", "owner1@example.com", User.Role.OWNER);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/users/search?role=USER&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(2);
        assertThat(response.getBody().get("totalElements")).isEqualTo(2);
    }

    @Test
    void searchByKeyword_shouldMatchUsernameAndEmail() {
        createUser("alice", "alice@sport.com", User.Role.USER);
        createUser("bob", "bob@sport.com", User.Role.USER);
        createUser("charlie", "charlie@other.com", User.Role.OWNER);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/users/search?q=sport&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(2);
        assertThat(response.getBody().get("totalElements")).isEqualTo(2);
    }

    @Test
    void searchWithPaging_shouldRespectPageSizeAndReportCorrectTotals() {
        for (int i = 1; i <= 5; i++) {
            createUser("player" + i, "player" + i + "@example.com", User.Role.USER);
        }

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/users/search?role=USER&page=0&size=3",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalElements")).isEqualTo(5);
        assertThat(response.getBody().get("totalPages")).isEqualTo(2);
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSize(3);
    }

    @Test
    void searchByRoleAndKeyword_shouldIntersectBothFilters() {
        createUser("alice", "alice@example.com", User.Role.USER);
        createUser("alicia", "alicia@example.com", User.Role.OWNER);
        createUser("bob", "bob@example.com", User.Role.USER);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/users/search?role=USER&q=alice&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalElements")).isEqualTo(1);
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content.get(0).get("username")).isEqualTo("alice");
    }

    @Test
    void searchWithEmptyResult_shouldReturnZeroElements_whenNoUsersMatchRole() {
        createUser("onlyUser", "only@example.com", User.Role.USER);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/api/users/search?role=ADMIN&page=0&size=10",
                HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("totalElements")).isEqualTo(0);
        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).isEmpty();
    }
}
