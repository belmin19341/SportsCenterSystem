package ba.nwt.apigateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtValidator", mock(JwtValidator.class));
        ReflectionTestUtils.setField(filter, "blacklist", mock(TokenBlacklistService.class));
        ReflectionTestUtils.setField(filter, "rateLimitService", mock(RateLimitService.class));
        ReflectionTestUtils.setField(filter, "rateLimitEnabled", false);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/facilities",
            "/api/facilities/42",
            "/api/equipment",
            "/api/equipment/facility/5"
    })
    void publicResourceGetRoutesBypassJwt(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(path).build()
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = ignored -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        assertThat(chainInvoked).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void protectedRoutesWithoutAuthAreRejected() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payments").build()
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = ignored -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        assertThat(chainInvoked).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void lookalikePrefixesDoNotBypassJwt() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/facilities-admin").build()
        );
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        GatewayFilterChain chain = ignored -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain))
                .verifyComplete();

        assertThat(chainInvoked).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
