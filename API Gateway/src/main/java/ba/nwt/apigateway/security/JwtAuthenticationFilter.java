package ba.nwt.apigateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Gateway filter — <b>authentication + identity propagation only</b>.
 *
 * <p>Responsibilities (kept intentionally small):
 * <ul>
 *   <li>verify the JWT signature, {@code iss}, {@code aud}, {@code exp}
 *       via {@link JwtValidator},</li>
 *   <li>reject revoked (logged-out) tokens via {@link TokenBlacklistService},</li>
 *   <li>reject {@code type=refresh} tokens on protected routes (refresh tokens
 *       may only be used on {@code /api/auth/refresh}),</li>
 *   <li>apply per-IP rate limit on {@code POST /api/auth/login},</li>
 *   <li>propagate identity to downstream services via {@code X-User-Id},
 *       {@code X-User-Name}, {@code X-User-Role}, {@code X-User-Roles}.</li>
 * </ul>
 *
 * <p><b>Authorization (RBAC, ownership, business rules) is NOT performed here.</b>
 * The gateway intentionally stays "dumb": it does centralized
 * <em>authentication</em>, while each downstream microservice performs
 * <em>decentralized authorization</em> on its own domain objects
 * (e.g. "user X may update only their own booking", "OWNER may edit only their
 * own facility"). That keeps domain knowledge inside the domain service and
 * avoids a fat, leaky gateway.</p>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private JwtValidator jwtValidator;

    @Autowired
    private TokenBlacklistService blacklist;

    @Autowired
    private RateLimitService rateLimitService;

    @Value("${security.ratelimit.login.enabled:true}")
    private boolean rateLimitEnabled;

    public JwtAuthenticationFilter() {
        super(JwtAuthenticationFilter.Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            HttpMethod method = request.getMethod();

            // ── Per-IP rate limit on /api/auth/login (POST) ────────────────────
            if (rateLimitEnabled
                    && HttpMethod.POST.equals(method)
                    && path.endsWith("/api/auth/login")) {
                String ip = clientIp(request);
                if (rateLimitService.isLimited(ip)) {
                    return onError(exchange,
                            "Too many login attempts. Please retry after "
                                    + rateLimitService.getWindowSeconds() + " seconds.",
                            HttpStatus.TOO_MANY_REQUESTS);
                }
            }

            // Public routes do not require a token
            if (isPublicRoute(path, method)) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || authHeader.isEmpty()) {
                log.warn("Missing authorization header for path: {}", path);
                return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
            }

            try {
                String token = jwtValidator.extractToken(authHeader);
                if (token == null) {
                    return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
                }

                // 1. signature + iss + aud + exp
                if (!jwtValidator.validateToken(token)) {
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }

                // 2. refresh tokens may not access protected resources
                String type = jwtValidator.getTypeFromToken(token);
                if (type == null || !"access".equalsIgnoreCase(type)) {
                    log.warn("Non-access token (type={}) used on protected route {}", type, path);
                    return onError(exchange,
                            "Refresh token cannot be used to access protected resources",
                            HttpStatus.UNAUTHORIZED);
                }

                // 3. revocation check (logged-out / forcibly-revoked tokens)
                String jti = jwtValidator.getJtiFromToken(token);
                if (blacklist.isBlacklisted(jti)) {
                    log.warn("Blacklisted token used: jti={}", jti);
                    return onError(exchange, "Token has been revoked (logged out)", HttpStatus.UNAUTHORIZED);
                }

                // 4. propagate identity — downstream services do authorization
                Long userId = jwtValidator.getUserIdFromToken(token);
                String username = jwtValidator.getUsernameFromToken(token);
                List<String> roles = jwtValidator.getRolesFromToken(token);
                String primaryRole = roles.isEmpty() ? "" : roles.get(0);

                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Name", username == null ? "" : username)
                        .header("X-User-Role", primaryRole)              // legacy single role
                        .header("X-User-Roles", String.join(",", roles)) // canonical multi-role
                        .build();

                log.info("Authenticated: user={}, roles={}, path={}", username, roles, path);
                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("Error validating token: {}", e.getMessage());
                return onError(exchange, "Token validation failed", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    /**
     * Public routes that bypass JWT validation.
     * <ul>
     *   <li>{@code /api/auth/login}, {@code /api/auth/register} — credential / account creation</li>
     *   <li>{@code /api/auth/refresh} — token exchange</li>
     *   <li>{@code /api/auth/logout}, {@code /api/auth/validate} — User Service inspects
     *       the Authorization header internally</li>
     *   <li>{@code GET /api/facilities/**}, {@code GET /api/equipment/**} — public catalog data</li>
     *   <li>swagger / openapi / actuator / health — operational endpoints</li>
     * </ul>
     */
    private boolean isPublicRoute(String path, HttpMethod method) {
        if (HttpMethod.GET.equals(method)
                && (matchesPathPrefix(path, "/api/facilities")
                || matchesPathPrefix(path, "/api/equipment"))) {
            return true;
        }
        return path.endsWith("/api/auth/login")
                || path.endsWith("/api/auth/register")
                || path.endsWith("/api/auth/refresh")
                || path.endsWith("/api/auth/logout")
                || path.endsWith("/api/auth/validate")
                || path.contains("/api-docs")
                || path.contains("/swagger-ui")
                || path.contains("/swagger-resources")
                || path.contains("/v3/api-docs")
                || path.endsWith("/health")
                || path.startsWith("/actuator");
    }

    private boolean matchesPathPrefix(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    /** Best-effort client IP extraction (X-Forwarded-For or remote address). */
    private String clientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        String errorLabel = switch (status) {
            case UNAUTHORIZED -> "Unauthorized";
            case FORBIDDEN -> "Forbidden";
            case TOO_MANY_REQUESTS -> "Too Many Requests";
            default -> "Error";
        };

        String errorBody = String.format(
                "{\"error\":\"%s\",\"status\":%d,\"message\":\"%s\"}",
                errorLabel, status.value(), message.replace("\"", "\\\"")
        );

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(errorBody.getBytes()))
        );
    }

    public static class Config { }
}
