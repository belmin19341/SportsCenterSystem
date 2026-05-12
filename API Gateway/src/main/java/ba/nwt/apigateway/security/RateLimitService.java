package ba.nwt.apigateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory IP-based rate limiter for sensitive endpoints (login).
 * Uses a fixed-window counter per IP. After the configured limit is reached
 * within the window, calls are rejected with HTTP 429 by the filter that uses
 * this service. Counters are reset every window-period by a scheduled task.
 *
 * Configuration (application.properties):
 *   security.ratelimit.login.max-requests = 5
 *   security.ratelimit.login.window-seconds = 60
 */
@Slf4j
@Service
public class RateLimitService {

    @Value("${security.ratelimit.login.max-requests:5}")
    private int maxRequests;

    @Value("${security.ratelimit.login.window-seconds:60}")
    private int windowSeconds;

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    /**
     * Returns true when the request from the given IP must be rejected
     * (i.e., the per-window counter has reached the configured limit).
     * Otherwise increments the counter and returns false.
     */
    public boolean isLimited(String clientIp) {
        if (clientIp == null) {
            clientIp = "unknown";
        }
        AtomicInteger counter = counters.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        if (count > maxRequests) {
            log.warn("Rate limit exceeded for IP {} ({} requests in current window)", clientIp, count);
            return true;
        }
        return false;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    /** Reset the counters at the configured window cadence. */
    @Scheduled(fixedDelayString = "#{${security.ratelimit.login.window-seconds:60} * 1000}")
    public void resetWindow() {
        if (!counters.isEmpty()) {
            log.debug("Rate limit window reset: clearing {} counters", counters.size());
        }
        counters.clear();
    }
}

