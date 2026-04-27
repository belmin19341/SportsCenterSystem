package ba.nwt.bookingservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * Loyalty is best-effort: a user-service outage MUST NOT roll back a confirmed booking.
 * The fallback therefore swallows the failure, logs a warning, and lets the orchestration
 * proceed. A later reconciliation job (or async retry in Z6) can replay missed points.
 */
public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClientFallback.class);
    private final Throwable cause;

    public UserServiceClientFallback(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public void addLoyaltyPoints(Long userId, int points) {
        log.warn("user-service unavailable; skipped adding {} loyalty points to user {} (cause: {})",
                points, userId, cause == null ? "n/a" : cause.toString());
    }

    @Component
    public static class Factory implements FallbackFactory<UserServiceClient> {
        @Override
        public UserServiceClient create(Throwable cause) {
            return new UserServiceClientFallback(cause);
        }
    }
}
