package ba.nwt.userservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Tiny HTTP client that calls the API Gateway's internal token-revoke endpoint.
 *
 * Uses the JDK 11+ {@link HttpClient} so no extra dependency is required.
 * Failures are logged but never propagated — the user's logout MUST succeed
 * even if the gateway is momentarily unreachable (the access token will then
 * naturally expire within {@code jwt.access.expiration} seconds anyway).
 */
@Slf4j
@Component
public class GatewayRevokeClient {

    @Value("${security.gateway.internal-revoke-url:http://localhost:8080/internal/revoke}")
    private String revokeUrl;

    @Value("${security.internal.secret:CHANGE_ME_INTERNAL_SECRET_DEV_ONLY}")
    private String internalSecret;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public void revoke(String jti, long expEpochMs) {
        if (jti == null || jti.isBlank()) return;
        String body = String.format("{\"jti\":\"%s\",\"exp\":%d}", jti, expEpochMs);
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(revokeUrl))
                    .timeout(Duration.ofSeconds(2))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Secret", internalSecret)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("Gateway revoke responded with status {}: {}", resp.statusCode(), resp.body());
            } else {
                log.info("Gateway revoke OK for jti={}", jti);
            }
        } catch (Exception e) {
            log.warn("Failed to call gateway revoke endpoint at {}: {}", revokeUrl, e.getMessage());
        }
    }
}

