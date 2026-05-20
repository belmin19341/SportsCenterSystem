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

import java.util.*;

/**
 * JWT Authentication Filter Factory for Spring Cloud Gateway
 * - validates JWT token signature and expiration
 * - rejects blacklisted (logged-out) tokens via {@link TokenBlacklistService}
 * - applies a per-IP rate limit on /api/auth/login via {@link RateLimitService}
 * - enforces role-based access control (RBAC) using prefix-matching
 * - propagates user info via {@code X-User-Id}, {@code X-User-Name}, {@code X-User-Role} headers
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

    /**
     * RBAC matrix: role -> array of path prefixes (or "**" for full access).
     * Path is matched via {@link #pathAllowed(String, String)} using:
     *   - exact match
     *   - "/foo/**" prefix match
     *   - "**" wildcard
     *
     * Note on USER permissions:
     *   USER can call all booking, payment and resource endpoints. Object-level
     *   ownership (e.g. "user X may only update their own booking") is the
     *   responsibility of each downstream service, which receives X-User-Id and
     *   X-User-Role headers from the gateway.
     */
    private static final Map<String, String[]> ROLE_PERMISSIONS = Map.of(
            "USER",  new String[]{
                    "/api/users/**",
                    "/api/bookings/**",
                    "/api/payments/**",
                    "/api/facilities/**",
                    "/api/equipment/**",
                    "/api/pricing-rules/**",
                    "/api/rentals/**",
                    "/api/reviews/**",
                    "/api/notifications/**",
                    "/api/loyalty/**",
                    "/api/achievements/**",
                    "/api/user-achievements/**"
            },
            "OWNER", new String[]{
                    "/api/users/**",
                    "/api/bookings/**",
                    "/api/payments/**",
                    "/api/facilities/**",
                    "/api/equipment/**",
                    "/api/pricing-rules/**",
                    "/api/rentals/**",
                    "/api/reviews/**",
                    "/api/notifications/**",
                    "/api/loyalty/**",
                    "/api/achievements/**",
                    "/api/user-achievements/**",
                    "/api/documents/**",
                    "/api/booking-users/**",
                    "/api/lb-demo/**"
            },
            "ADMIN", new String[]{ "**" }
    );

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

            // Extract authorization header
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || authHeader.isEmpty()) {
                log.warn("Missing authorization header for path: {}", path);
                return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
            }

            try {
                String token = jwtValidator.extractToken(authHeader);
                if (token == null) {
                    log.warn("Invalid authorization header format");
                    return onError(exchange, "Invalid authorization header", HttpStatus.UNAUTHORIZED);
                }

                if (!jwtValidator.validateToken(token)) {
                    log.warn("Invalid or expired token");
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }

                // Reject revoked (logged-out) tokens
                String jti = jwtValidator.getJtiFromToken(token);
                if (blacklist.isBlacklisted(jti)) {
                    log.warn("Blacklisted token used: jti={}", jti);
                    return onError(exchange, "Token has been revoked (logged out)", HttpStatus.UNAUTHORIZED);
                }

                // Extract user information from token
                Long userId = jwtValidator.getUserIdFromToken(token);
                String username = jwtValidator.getUsernameFromToken(token);
                String role = jwtValidator.getRoleFromToken(token);

                // Refresh tokens may not be used as access tokens (only on /api/auth/refresh)
                Object type = jwtValidator.getAllClaims(token).get("type");
                if (type != null && "refresh".equalsIgnoreCase(type.toString())) {
                    log.warn("Refresh token used as access token by user '{}'", username);
                    return onError(exchange, "Refresh token cannot be used to access protected resources",
                            HttpStatus.UNAUTHORIZED);
                }

                // Check role-based access control
                if (!hasAccessToPath(role, path)) {
                    log.warn("User '{}' with role '{}' attempting unauthorized access to: {}",
                            username, role, path);
                    return onError(exchange, "Insufficient permissions for this resource", HttpStatus.FORBIDDEN);
                }

                // Add user information to request headers
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId.toString())
                        .header("X-User-Name", username)
                        .header("X-User-Role", role)
                        .build();

                log.info("User authenticated: username={}, role={}, path={}", username, role, path);

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("Error validating token: {}", e.getMessage());
                return onError(exchange, "Token validation failed", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    /**
     * Public routes that bypass JWT validation.
     * - /api/auth/login, /api/auth/refresh — credential / token exchange
     * - /api/auth/logout — revokes the bearer token; user-service inspects the
     *   Authorization header internally and tolerates missing / invalid tokens
     * - /api/auth/validate — used by clients to check token state
     * - GET /api/facilities/**, GET /api/equipment/** — public discovery data
     * - swagger / openapi / actuator / health — operational endpoints
     */
    private boolean isPublicRoute(String path, HttpMethod method) {
        if (HttpMethod.GET.equals(method)
                && (matchesPathPrefix(path, "/api/facilities")
                || matchesPathPrefix(path, "/api/equipment"))) {
            return true;
        }

        return path.endsWith("/api/auth/login")
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

    /**
     * Check if user role has access to the requested path
     */
    private boolean hasAccessToPath(String role, String path) {
        if (role == null) {
            return false;
        }
        String[] patterns = ROLE_PERMISSIONS.get(role.toUpperCase(Locale.ROOT));
        if (patterns == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (pathAllowed(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /** Match "/foo/**", "/foo/bar" or "**" against the request path. */
    private boolean pathAllowed(String pattern, String path) {
        if ("**".equals(pattern) || "/**".equals(pattern)) {
            return true;
        }
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return matchesPathPrefix(path, prefix);
        }
        return pattern.equals(path);
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

    /**
     * Handle authentication/authorization error
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        // Uniform error format aligned with downstream services' ApiError shape:
        //   { "error": "<short error label>", "status": <int>, "message": "<details>" }
        String errorLabel;
        switch (status) {
            case UNAUTHORIZED -> errorLabel = "Unauthorized";
            case FORBIDDEN -> errorLabel = "Forbidden";
            case TOO_MANY_REQUESTS -> errorLabel = "Too Many Requests";
            default -> errorLabel = "Error";
        }

        String errorBody = String.format(
                "{\"error\":\"%s\",\"status\":%d,\"message\":\"%s\"}",
                errorLabel, status.value(), message.replace("\"", "\\\"")
        );

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(errorBody.getBytes()))
        );
    }

    /**
     * Configuration class for the filter
     */
    public static class Config {
    }
}
