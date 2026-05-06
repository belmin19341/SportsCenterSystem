package ba.nwt.apigateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JWT blacklist service.
 * Holds revoked JTI (JWT-ID) values together with their original expiry epoch (ms).
 * A scheduled task purges expired entries so the map stays bounded.
 *
 * No Redis is required - this lives inside the API Gateway (the single entry-point
 * that validates JWTs before routing), so each request only adds an O(1) map lookup.
 * If the gateway restarts, the blacklist is lost; tokens still expire naturally
 * via their short {@code exp} (15 min for access, 7 days for refresh).
 */
@Slf4j
@Service
public class TokenBlacklistService {

    /** jti -&gt; token original expiry in epoch milliseconds. */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void revoke(String jti, long expiresAt) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        blacklist.put(jti, expiresAt);
        log.info("Token revoked: jti={}, expiresAt={}", jti, expiresAt);
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) {
            return false;
        }
        Long exp = blacklist.get(jti);
        if (exp == null) {
            return false;
        }
        if (exp < System.currentTimeMillis()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    public int size() {
        return blacklist.size();
    }

    /** Periodically purge expired entries (every 5 minutes). */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(e -> e.getValue() < now);
        int after = blacklist.size();
        if (before != after) {
            log.info("Token blacklist cleanup: removed {} expired entries (size {} -> {})",
                    before - after, before, after);
        }
    }
}
