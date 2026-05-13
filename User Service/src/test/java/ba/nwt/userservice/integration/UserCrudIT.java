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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for User CRUD operations.
 * Full Spring context with H2 in-memory database — no mocks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserCrudIT {

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

    private UserRequestDTO buildRequest(String username, String email) {
        return UserRequestDTO.builder()
                .username(username)
                .email(email)
                .password("password123")
                .role(User.Role.USER)
                .phone("+38762111222")
                .build();
    }

    @Test
    void createUser_shouldReturn201AndPersistToDatabase() {
        UserRequestDTO request = buildRequest("johndoe", "john@example.com");

        ResponseEntity<UserResponseDTO> response =
                restTemplate.postForEntity("/api/users", request, UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isPositive();
        assertThat(response.getBody().getUsername()).isEqualTo("johndoe");
        assertThat(response.getBody().getEmail()).isEqualTo("john@example.com");
        assertThat(response.getBody().getRole()).isEqualTo(User.Role.USER);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void getUserById_shouldReturn200WithPersistedData() {
        UserResponseDTO created =
                restTemplate.postForObject("/api/users", buildRequest("janedoe", "jane@example.com"), UserResponseDTO.class);

        ResponseEntity<UserResponseDTO> response =
                restTemplate.getForEntity("/api/users/" + created.getId(), UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("janedoe");
        assertThat(response.getBody().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void getUserByUsername_shouldReturn200WithMatchingUser() {
        restTemplate.postForObject("/api/users", buildRequest("alice", "alice@example.com"), UserResponseDTO.class);

        ResponseEntity<UserResponseDTO> response =
                restTemplate.getForEntity("/api/users/username/alice", UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("alice");
        assertThat(response.getBody().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void updateUser_shouldReturn200WithModifiedData() {
        UserResponseDTO created =
                restTemplate.postForObject("/api/users", buildRequest("original", "orig@example.com"), UserResponseDTO.class);

        UserRequestDTO updateRequest = UserRequestDTO.builder()
                .username("updated")
                .email("updated@example.com")
                .password("newpassword123")
                .role(User.Role.OWNER)
                .build();

        ResponseEntity<UserResponseDTO> response = restTemplate.exchange(
                "/api/users/" + created.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                UserResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUsername()).isEqualTo("updated");
        assertThat(response.getBody().getEmail()).isEqualTo("updated@example.com");
        assertThat(response.getBody().getRole()).isEqualTo(User.Role.OWNER);
    }

    @Test
    void deleteUser_shouldReturn204AndSubsequentGetReturns404() {
        UserResponseDTO created =
                restTemplate.postForObject("/api/users", buildRequest("todelete", "delete@example.com"), UserResponseDTO.class);

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/users/" + created.getId(), HttpMethod.DELETE, null, Void.class);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse =
                restTemplate.getForEntity("/api/users/" + created.getId(), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(userRepository.existsById(created.getId())).isFalse();
    }

    @Test
    void getUser_shouldReturn404_whenIdDoesNotExist() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/users/999999", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAllUsers_shouldReturnAllPersistedUsers() {
        restTemplate.postForObject("/api/users", buildRequest("user1", "u1@example.com"), UserResponseDTO.class);
        restTemplate.postForObject("/api/users", buildRequest("user2", "u2@example.com"), UserResponseDTO.class);

        ResponseEntity<List<UserResponseDTO>> response = restTemplate.exchange(
                "/api/users", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<UserResponseDTO>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }
}
