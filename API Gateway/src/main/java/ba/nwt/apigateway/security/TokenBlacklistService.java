package ba.nwt.apigateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory blacklist of revoked <b>access</b> tokens (by {@code jti}).
 *
 * <p><b>Data:</b> {@code ConcurrentHashMap<jti, expEpochMs>}. Lookup is O(1).</p>
 *
 * <p><b>Lifecycle / cleanup:</b>
 * <ul>
 *   <li><b>Lazy eviction:</b> {@link #isBlacklisted(String)} removes the entry
 *       as soon as its original {@code exp} is in the past, so the map self-heals
 *       on read.</li>
 *   <li><b>Scheduled sweep:</b> {@link #cleanupExpired()} runs every 5 minutes
 *       and bulk-removes all expired entries. This keeps memory bounded even
 *       for JTIs that are revoked but never checked again before they expire.</li>
 *   <li><b>Upper bound on memory:</b> at most the number of access tokens that
 *       are simultaneously (a) revoked and (b) not yet past their {@code exp}.
 *       With a 15-minute access TTL this is small by construction.</li>
 * </ul>
 *
 * <p><b>Restart behavior:</b> the blacklist is in-memory and is therefore lost
 * when the gateway restarts. Practical impact is bounded: a revoked access
 * token could be accepted again for at most the remainder of its {@code exp}
 * window (worst case ≈ 15 min, the access TTL). For an academic project this
 * trade-off is acceptable; a production system would back this with Redis (or a
 * DB) so that revocations survive restarts and are shared across multiple
 * gateway instances. The same interface ({@code revoke} / {@code isBlacklisted})
 * would not change — only the backing store.</p>
 *
 * <p><b>Not for refresh tokens.</b> Refresh tokens never reach the gateway on
 * protected routes; they are revoked locally in
 * {@code ba.nwt.userservice.security.RefreshTokenBlacklist}.</p>
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
