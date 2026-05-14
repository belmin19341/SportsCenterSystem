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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for User input validation and uniqueness constraints.
 * Verifies that invalid or duplicate data is rejected at the HTTP layer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserValidationIT {

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

    private UserRequestDTO validRequest() {
        return UserRequestDTO.builder()
                .username("validuser")
                .email("valid@example.com")
                .password("password123")
                .role(User.Role.USER)
                .build();
    }

    @Test
    void createUser_shouldReturn400_whenUsernameIsTooShort() {
        UserRequestDTO request = validRequest();
        request.setUsername("ab");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/users", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void createUser_shouldReturn400_whenEmailIsInvalid() {
        UserRequestDTO request = validRequest();
        request.setEmail("not-an-email");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/users", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void createUser_shouldReturn400_whenPasswordIsTooShort() {
        UserRequestDTO request = validRequest();
        request.setPassword("123");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/users", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void createUser_shouldReturn400_whenRoleIsMissing() {
        UserRequestDTO request = UserRequestDTO.builder()
                .username("validuser")
                .email("valid@example.com")
                .password("password123")
                .role(null)
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/users", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createUser_shouldReturn400_whenUsernameIsDuplicate() {
        UserRequestDTO first = UserRequestDTO.builder()
                .username("duplicate")
                .email("first@example.com")
                .password("password123")
                .role(User.Role.USER)
                .build();
        restTemplate.postForObject("/api/users", first, UserResponseDTO.class);

        UserRequestDTO second = UserRequestDTO.builder()
                .username("duplicate")
                .email("second@example.com")
                .password("password123")
                .role(User.Role.USER)
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/users", second, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void createUser_shouldReturn400_whenEmailIsDuplicate() {
        UserRequestDTO first = UserRequestDTO.builder()
                .username("user1")
                .email("shared@example.com")
                .password("password123")
                .role(User.Role.USER)
                .build();
        restTemplate.postForObject("/api/users", first, UserResponseDTO.class);

        UserRequestDTO second = UserRequestDTO.builder()
                .username("user2")
                .email("shared@example.com")
                .password("password123")
                .role(User.Role.USER)
                .build();

        ResponseEntity<String> response = restTemplate.postForEntity("/api/users", second, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void updateUser_shouldReturn404_whenUserDoesNotExist() {
        UserRequestDTO request = validRequest();

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/users/999999",
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(request),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
