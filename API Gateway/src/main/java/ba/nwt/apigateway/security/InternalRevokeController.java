package ba.nwt.apigateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Internal endpoint used ONLY by the User Service to revoke tokens on logout.
 *
 * Protected by a shared secret header ({@code X-Internal-Secret}) so that
 * outside callers cannot revoke arbitrary JTIs even though the gateway is
 * publicly reachable. The secret is sourced from the {@code security.internal.secret}
 * property (env: {@code INTERNAL_SECRET}).
 *
 * The body is intentionally tiny:
 *   { "jti": "...", "exp": 1778163055000 }
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalRevokeController {

    private final TokenBlacklistService blacklist;

    @Value("${security.internal.secret:CHANGE_ME_INTERNAL_SECRET_DEV_ONLY}")
    private String internalSecret;

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(
            @RequestHeader(value = "X-Internal-Secret", required = false) String headerSecret,
            @RequestBody RevokeRequest body) {

        if (headerSecret == null || !headerSecret.equals(internalSecret)) {
            log.warn("Unauthorized internal revoke attempt (secret missing/mismatch)");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Forbidden", "status", 403,
                            "message", "Invalid internal secret"));
        }
        if (body == null || body.jti == null || body.jti.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad Request", "status", 400,
                            "message", "jti is required"));
        }

        long exp = body.exp == null ? System.currentTimeMillis() + 60_000L : body.exp;
        blacklist.revoke(body.jti, exp);

        return ResponseEntity.ok(Map.of(
                "revoked", true,
                "jti", body.jti,
                "blacklistSize", blacklist.size()
        ));
    }

    public static class RevokeRequest {
        public String jti;
        public Long exp;
    }
}

