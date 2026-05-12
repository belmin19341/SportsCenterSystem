package ba.nwt.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for POST /api/auth/refresh — exchanges a long-lived refresh token
 * for a fresh access token.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequestDTO {

    @NotBlank(message = "refresh_token is required")
    @com.fasterxml.jackson.annotation.JsonProperty("refresh_token")
    private String refreshToken;
}

