package ba.nwt.apigateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * JWT Authentication Filter Factory for Spring Cloud Gateway
 * Validates JWT tokens and adds user information to request headers
 * Also enforces role-based access control (RBAC)
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private JwtValidator jwtValidator;

    // Role-based route permissions
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.ofEntries(
            // USER role - limited access
            Map.entry("USER", new HashSet<>(Arrays.asList(
                    "/api/users/profile",
                    "/api/users/me",
                    "/api/bookings/my-bookings",
                    "/api/bookings/create",
                    "/api/resources/list",
                    "/api/payments/my-payments"
            ))),
            // OWNER role - facility management
            Map.entry("OWNER", new HashSet<>(Arrays.asList(
                    "/api/users/profile",
                    "/api/users/me",
                    "/api/resources/**",
                    "/api/bookings/**",
                    "/api/payments/**"
            ))),
            // ADMIN role - full access
            Map.entry("ADMIN", new HashSet<>(Arrays.asList(
                    "/api/**"
            )))
    );

    public JwtAuthenticationFilter() {
        super(JwtAuthenticationFilter.Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            // Routes that don't require authentication
            if (isPublicRoute(path)) {
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

                // Extract user information from token
                Long userId = jwtValidator.getUserIdFromToken(token);
                String username = jwtValidator.getUsernameFromToken(token);
                String role = jwtValidator.getRoleFromToken(token);

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
     * Check if route is public (doesn't require authentication)
     */
    private boolean isPublicRoute(String path) {
        return path.contains("/auth/login") ||
                path.contains("/auth/validate") ||
                path.contains("/api-docs") ||
                path.contains("/swagger-ui") ||
                path.contains("/health") ||
                path.contains("/actuator");
    }

    /**
     * Check if user role has access to the requested path
     */
    private boolean hasAccessToPath(String role, String path) {
        Set<String> permissions = ROLE_PERMISSIONS.get(role);
        if (permissions == null) {
            return false;
        }

        // Check for exact or wildcard matches
        for (String permission : permissions) {
            if (permission.equals(path) || matchesWildcard(permission, path)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if path matches wildcard permission pattern
     */
    private boolean matchesWildcard(String pattern, String path) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return false;
    }

    /**
     * Handle authentication/authorization error
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        String errorBody = String.format(
                "{\"error\":\"%s\",\"status\":%d}",
                message,
                status.value()
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
