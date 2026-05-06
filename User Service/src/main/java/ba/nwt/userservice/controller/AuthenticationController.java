package ba.nwt.userservice.controller;

import ba.nwt.userservice.dto.AuthResponseDTO;
import ba.nwt.userservice.dto.LoginRequestDTO;
import ba.nwt.userservice.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Endpoints for user authentication and JWT token management
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
