package ba.nwt.userservice.service;

import ba.nwt.userservice.dto.AuthResponseDTO;
import ba.nwt.userservice.dto.LoginRequestDTO;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.repository.UserRepository;
import ba.nwt.userservice.security.GatewayRevokeClient;
import ba.nwt.userservice.security.JwtTokenProvider;
import ba.nwt.userservice.security.RefreshTokenBlacklist;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication Service
 *
 * Responsibilities:
 *   - login → access + refresh token
 *   - refresh → new access (and rotated refresh) token
 *   - logout → revoke access JTI on the gateway and refresh JTI locally
 *   - validate → quick token check
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final GatewayRevokeClient gatewayRevokeClient;
    private final RefreshTokenBlacklist refreshBlacklist;

    /* ────────────────────────────────────────────────────────────────────── */

    public AuthResponseDTO authenticate(LoginRequestDTO loginRequest) {
        log.info("Login attempt for username: {}", loginRequest.getUsername());

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", loginRequest.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }

        String access = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        String refresh = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getUsername(), user.getRole().name());

        log.info("Login successful for user: {}", user.getUsername());
        return new AuthResponseDTO(
                access,
                refresh,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                jwtTokenProvider.getAccessTokenExpirationSec(),
                jwtTokenProvider.getRefreshTokenExpirationSec()
        );
    }

    /* ────────────────────────────────────────────────────────────────────── */

    /**
     * Exchange a refresh token for a new access token. Implements refresh-token
     * rotation: the old refresh JTI is revoked and a new one is issued.
     */
    public AuthResponseDTO refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("refresh_token is required");
        }
        if (!Boolean.TRUE.equals(jwtTokenProvider.validateToken(refreshToken))) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }
        String type = jwtTokenProvider.getTypeFromToken(refreshToken);
        if (!"refresh".equalsIgnoreCase(type)) {
            throw new BadCredentialsException("Provided token is not a refresh token");
        }
        String oldJti = jwtTokenProvider.getJtiFromToken(refreshToken);
        if (refreshBlacklist.isBlacklisted(oldJti)) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Rotate: revoke old refresh, issue new pair
        refreshBlacklist.revoke(oldJti, jwtTokenProvider.getExpirationDateFromToken(refreshToken).getTime());

        String newAccess = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
        String newRefresh = jwtTokenProvider.generateRefreshToken(
                user.getId(), user.getUsername(), user.getRole().name());

        log.info("Refresh token rotated for user: {}", user.getUsername());
        return new AuthResponseDTO(
                newAccess, newRefresh,
                user.getId(), user.getUsername(), user.getEmail(), user.getRole().name(),
                jwtTokenProvider.getAccessTokenExpirationSec(),
                jwtTokenProvider.getRefreshTokenExpirationSec()
        );
    }

    /* ────────────────────────────────────────────────────────────────────── */

    /**
     * Revoke the supplied access (and optional refresh) tokens.
     *
     * @param accessToken          the bearer access token (required)
     * @param refreshToken optional refresh token (will also be revoked if present)
     */
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                if (jwtTokenProvider.validateToken(accessToken)) {
                    String jti = jwtTokenProvider.getJtiFromToken(accessToken);
                    long exp = jwtTokenProvider.getExpirationDateFromToken(accessToken).getTime();
                    gatewayRevokeClient.revoke(jti, exp);
                    log.info("Logout: access token jti={} revoked at gateway", jti);
                }
            } catch (Exception e) {
                log.warn("Logout: failed to revoke access token: {}", e.getMessage());
            }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                if (jwtTokenProvider.validateToken(refreshToken)
                        && "refresh".equalsIgnoreCase(jwtTokenProvider.getTypeFromToken(refreshToken))) {
                    String jti = jwtTokenProvider.getJtiFromToken(refreshToken);
                    long exp = jwtTokenProvider.getExpirationDateFromToken(refreshToken).getTime();
                    refreshBlacklist.revoke(jti, exp);
                }
            } catch (Exception e) {
                log.warn("Logout: failed to revoke refresh token: {}", e.getMessage());
            }
        }
    }

    /* ────────────────────────────────────────────────────────────────────── */

    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    public Long getUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    public String getRoleFromToken(String token) {
        return jwtTokenProvider.getRoleFromToken(token);
    }
}
