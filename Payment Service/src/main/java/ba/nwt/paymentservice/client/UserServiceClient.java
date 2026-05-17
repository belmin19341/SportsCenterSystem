package ba.nwt.paymentservice.client;

import ba.nwt.paymentservice.client.dto.UserView;
import ba.nwt.paymentservice.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Synchronous Feign client for User Service.
 * Used in PaymentService#create to validate that the paying user exists
 * before any payment record is persisted.
 *
 * Failure semantics:
 *   - User not found (404)  → DownstreamBadRequestException → GlobalExceptionHandler → 400
 *   - User Service down (5xx) → DownstreamUnavailableException → GlobalExceptionHandler → 503
 */
@FeignClient(
        name = "user-service",
        configuration = FeignConfig.class,
        fallbackFactory = UserServiceClientFallback.Factory.class
)
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    UserView getUser(@PathVariable("id") Long id);
}
