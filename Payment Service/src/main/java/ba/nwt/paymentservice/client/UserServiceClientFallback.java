package ba.nwt.paymentservice.client;

import ba.nwt.paymentservice.client.dto.UserView;
import ba.nwt.paymentservice.exception.DownstreamUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClientFallback.class);
    private final Throwable cause;

    public UserServiceClientFallback(Throwable cause) {
        this.cause = cause;
    }

    @Override
    public UserView getUser(Long id) {
        log.warn("user-service unavailable while validating user {}: {}",
                id, cause == null ? "n/a" : cause.toString());
        throw new DownstreamUnavailableException("user-service",
                "User service is unavailable; cannot validate user " + id, cause);
    }

    @Component
    public static class Factory implements FallbackFactory<UserServiceClient> {
        @Override
        public UserServiceClient create(Throwable cause) {
            return new UserServiceClientFallback(cause);
        }
    }
}
