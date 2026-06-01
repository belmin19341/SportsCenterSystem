package ba.nwt.userservice.controller;

import ba.nwt.userservice.dto.AuthResponseDTO;
import ba.nwt.userservice.dto.LoginRequestDTO;
import ba.nwt.userservice.dto.RegisterRequestDTO;
import ba.nwt.userservice.dto.RefreshTokenRequestDTO;
import ba.nwt.userservice.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication Controller
 *
 * Endpoints:
 *   POST /api/auth/login    — exchange username+password for access + refresh tokens
 *   POST /api/auth/refresh  — exchange a valid refresh token for a new access token
 *   POST /api/auth/register — register a new user and return JWT token
 *   POST /api/auth/logout   — revoke the bearer (and optional refresh) token
 *   POST /api/auth/validate — check whether a bearer token is still valid
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * Login endpoint - authenticate user and return JWT token
     * 
     * @param loginRequest Login credentials (username, password)
     * @return JWT token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        AuthResponseDTO response = authenticationService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Register endpoint - create a new user and return JWT token for immediate login
     *
     * @param registerRequest User registration details (username, email, password)
     * @return JWT token and user information
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        AuthResponseDTO response = authenticationService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Refresh token endpoint - exchange a valid refresh token for a new access token
     * 
     * @param body Refresh token request containing the refresh token
     * @return New access token and user information
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(@Valid @RequestBody RefreshTokenRequestDTO body) {
        return ResponseEntity.ok(authenticationService.refresh(body.getRefreshToken()));
    }

    /**
     * Logout endpoint - revoke the bearer and optional refresh token
     * 
     * @param authHeader Authorization header containing "Bearer <token>"
     * @param body Optional refresh token request
     * @return Logout status message
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) RefreshTokenRequestDTO body) {

        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        String refreshToken = body == null ? null : body.getRefreshToken();
        authenticationService.logout(accessToken, refreshToken);

        return ResponseEntity.status(HttpStatus.OK).body(Map.of(
                "logout", true,
                "message", "Tokens revoked. Please discard local copies."
        ));
    }

    /**
     * Validate token endpoint - check if token is still valid
     * 
     * @param authHeader Authorization header containing "Bearer <token>"
     * @return Token validity status
     */
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(new TokenValidationResponse(false, "Invalid authorization header"));
        }

        String token = authHeader.substring(7);
        boolean isValid = authenticationService.validateToken(token);
        
        if (isValid) {
            return ResponseEntity.ok(new TokenValidationResponse(true, "Token is valid"));
        } else {
            return ResponseEntity.ok(new TokenValidationResponse(false, "Token is invalid or expired"));
        }
    }

    /**
     * DTO for token validation response
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.AllArgsConstructor
    public static class TokenValidationResponse {
        private boolean valid;
        private String message;
    }
}
