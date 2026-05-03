package ba.nwt.bookingservice.client;

import ba.nwt.bookingservice.exception.DownstreamUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Z5 — Verifies the per-client fallback behaviour.
 * <ul>
 *   <li>Resource and Payment fallbacks must throw a typed exception (caller cannot proceed).</li>
 *   <li>User Service fallback must swallow failures (loyalty is best-effort).</li>
 * </ul>
 */
class FeignFallbacksTest {

    private final RuntimeException cause = new RuntimeException("connection refused");

    @Test
    void resourceFallback_throwsDownstreamUnavailable() {
        ResourceServiceClientFallback fb = new ResourceServiceClientFallback(cause);

        assertThatThrownBy(() -> fb.getFacility(1L))
                .isInstanceOf(DownstreamUnavailableException.class)
                .hasMessageContaining("Resource service is unavailable");

        assertThatThrownBy(() -> fb.calculatePrice(1L, LocalDateTime.now(), LocalDateTime.now().plusHours(1)))
                .isInstanceOf(DownstreamUnavailableException.class);
    }

    @Test
    void paymentFallback_throwsDownstreamUnavailable() {
        PaymentServiceClientFallback fb = new PaymentServiceClientFallback(cause);

        assertThatThrownBy(() -> fb.createPayment(null))
                .isInstanceOf(DownstreamUnavailableException.class)
                .hasMessageContaining("Payment service is unavailable");
    }

    @Test
    void userFallback_swallowsFailure_loyaltyIsBestEffort() {
        UserServiceClientFallback fb = new UserServiceClientFallback(cause);

        // No exception expected — the user-service outage must NOT propagate.
        fb.addLoyaltyPoints(99L, 50);

        assertThat(true).isTrue();
    }

    @Test
    void factoriesProduceFallbackInstances() {
        assertThat(new ResourceServiceClientFallback.Factory().create(cause))
                .isInstanceOf(ResourceServiceClientFallback.class);
        assertThat(new PaymentServiceClientFallback.Factory().create(cause))
                .isInstanceOf(PaymentServiceClientFallback.class);
        assertThat(new UserServiceClientFallback.Factory().create(cause))
                .isInstanceOf(UserServiceClientFallback.class);
    }
}
