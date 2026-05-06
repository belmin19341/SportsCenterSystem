package ba.nwt.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * DTO for authentication response with JWT token
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    private Long userId;

    private String username;

    private String email;

    private String role;

    @JsonProperty("expires_in")
    private Long expiresIn;  // Token expiration time in seconds

    public AuthResponseDTO(String accessToken, Long userId, String username, String email, String role, Long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
    }
}
