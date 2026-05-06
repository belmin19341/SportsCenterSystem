package ba.nwt.userservice.service;

import ba.nwt.userservice.dto.AuthResponseDTO;
import ba.nwt.userservice.dto.LoginRequestDTO;
import ba.nwt.userservice.exception.ResourceNotFoundException;
import ba.nwt.userservice.model.User;
import ba.nwt.userservice.repository.UserRepository;
import ba.nwt.userservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication Service
 * Handles user login and JWT token generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.expiration:86400}")
    private long jwtExpirationMs;

    /**
     * Authenticate user and generate JWT token
     */
    public AuthResponseDTO authenticate(LoginRequestDTO loginRequest) {
        log.info("Login attempt for username: {}", loginRequest.getUsername());
        try {
            // Find user by username
            log.debug("Searching for user: {}", loginRequest.getUsername());
            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + loginRequest.getUsername()));
            log.debug("User found: {} with ID: {}", user.getUsername(), user.getId());

            // Validate password
            log.debug("Validating password for user: {}", user.getUsername());
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                log.warn("Invalid password for user: {}", user.getUsername());
                throw new IllegalArgumentException("Invalid password");
            }
            log.debug("Password validated successfully for user: {}", user.getUsername());

            // Generate JWT token
            log.debug("Generating JWT token for user: {} with role: {}", user.getUsername(), user.getRole());
            String token = jwtTokenProvider.generateToken(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name()
            );
            log.debug("JWT token generated successfully for user: {}", user.getUsername());

            // Return authentication response
            AuthResponseDTO response = new AuthResponseDTO(
                    token,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name(),
                    jwtExpirationMs
            );
            log.info("Login successful for user: {}", user.getUsername());
            return response;
        } catch (Exception e) {
            log.error("Login failed for user: {}", loginRequest.getUsername(), e);
            throw e;
        }
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }

    /**
     * Extract user ID from token
     */
    public Long getUserIdFromToken(String token) {
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    /**
     * Extract role from token
     */
    public String getRoleFromToken(String token) {
        return jwtTokenProvider.getRoleFromToken(token);
    }
}
