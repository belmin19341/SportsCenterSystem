package ba.nwt.userservice.service;

import ba.nwt.userservice.dto.AuthResponseDTO;
import ba.nwt.userservice.dto.LoginRequestDTO;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.repository.UserLoyaltyRepository;
import ba.nwt.userservice.repository.UserRepository;
import ba.nwt.userservice.security.GatewayRevokeClient;
import ba.nwt.userservice.security.JwtTokenProvider;
import ba.nwt.userservice.security.RefreshTokenBlacklist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLoyaltyRepository userLoyaltyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private GatewayRevokeClient gatewayRevokeClient;

    @Mock
    private RefreshTokenBlacklist refreshTokenBlacklist;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(
                userRepository,
                userLoyaltyRepository,
                passwordEncoder,
                jwtTokenProvider,
                gatewayRevokeClient,
                refreshTokenBlacklist
        );
    }

    @Test
    void authenticate_shouldUpgradeLegacyPlainTextPassword() {
        User user = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@sportcenter.ba")
                .passwordHash("password123")
                .role(User.Role.ADMIN)
                .build();

        LoginRequestDTO request = LoginRequestDTO.builder()
                .username("admin")
                .password("password123")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$encoded");
        when(jwtTokenProvider.generateAccessToken(1L, "admin", "admin@sportcenter.ba", "ADMIN")).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(1L, "admin", "ADMIN")).thenReturn("refresh");
        when(jwtTokenProvider.getAccessTokenExpirationSec()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationSec()).thenReturn(604800L);

        AuthResponseDTO response = authenticationService.authenticate(request);

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$encoded");
        verify(userRepository).save(user);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void authenticate_shouldAcceptBcryptPasswordWithoutRehashing() {
        User user = User.builder()
                .id(2L)
                .username("john_doe")
                .email("john@example.com")
                .passwordHash("$2a$existing")
                .role(User.Role.USER)
                .build();

        LoginRequestDTO request = LoginRequestDTO.builder()
                .username("john_doe")
                .password("password123")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$existing")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(2L, "john_doe", "john@example.com", "USER")).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(2L, "john_doe", "USER")).thenReturn("refresh");
        when(jwtTokenProvider.getAccessTokenExpirationSec()).thenReturn(900L);
        when(jwtTokenProvider.getRefreshTokenExpirationSec()).thenReturn(604800L);

        AuthResponseDTO response = authenticationService.authenticate(request);

        assertThat(response.getUsername()).isEqualTo("john_doe");
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_shouldRejectInvalidLegacyPassword() {
        User user = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@sportcenter.ba")
                .passwordHash("different")
                .role(User.Role.ADMIN)
                .build();

        LoginRequestDTO request = LoginRequestDTO.builder()
                .username("admin")
                .password("password123")
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(userRepository, never()).save(any());
    }
}
