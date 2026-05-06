package ba.nwt.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * DTO for authentication response with access + refresh JWT tokens.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    private Long userId;

    private String username;

    private String email;

    private String role;

    /** Access-token TTL in seconds. */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /** Refresh-token TTL in seconds. */
    @JsonProperty("refresh_expires_in")
    private Long refreshExpiresIn;

    public AuthResponseDTO(String accessToken, String refreshToken,
                           Long userId, String username, String email, String role,
                           Long expiresIn, Long refreshExpiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
    }
}
