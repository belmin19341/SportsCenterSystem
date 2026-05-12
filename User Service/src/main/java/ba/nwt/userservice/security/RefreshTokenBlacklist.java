package ba.nwt.userservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local in-memory blacklist for REFRESH tokens.
 *
 * <p>Access tokens are validated/blacklisted at the API Gateway, but refresh
 * tokens never leave the User Service (the only place that consumes them is
 * {@code POST /api/auth/refresh}). Storing revoked refresh JTIs here is enough
 * to make logout fully effective.</p>
 */
@Slf4j
@Component
public class RefreshTokenBlacklist {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public void revoke(String jti, long expiresAtMs) {
        if (jti == null || jti.isBlank()) return;
        blacklist.put(jti, expiresAtMs);
        log.info("Refresh token revoked: jti={}, expiresAt={}", jti, expiresAtMs);
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        Long exp = blacklist.get(jti);
        if (exp == null) return false;
        if (exp < System.currentTimeMillis()) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    public int size() { return blacklist.size(); }

    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void cleanup() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(e -> e.getValue() < now);
        if (before != blacklist.size()) {
            log.info("Refresh blacklist cleanup: {} -> {}", before, blacklist.size());
        }
    }
}

