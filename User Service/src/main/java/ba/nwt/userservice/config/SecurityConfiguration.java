package ba.nwt.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;

/**
 * Spring Security Configuration for User Service.
 *
 * <p>The API Gateway is the security boundary: it validates JWT signature,
 * expiration and blacklist before forwarding any request, and propagates
 * {@code X-User-Id}, {@code X-User-Name}, {@code X-User-Role} headers.</p>
 *
 * <p>Therefore the User Service itself runs with permitAll() on all endpoints.
 * In production the service port (8081) MUST NOT be exposed publicly — only the
 * gateway port (8080) should be reachable from outside.</p>
 */
@Configuration
@EnableWebSecurity
@Order(0)
public class SecurityConfiguration {

    /**
     * Configure security filter chain
     * - Disable CSRF (not needed for stateless API)
     * - Set session policy to stateless (JWT doesn't use sessions)
     * - Permit all requests (API Gateway handles authentication)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for API
            .csrf(csrf -> csrf.disable())
            // Set session policy to stateless (JWT doesn't use sessions)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // All requests are permitted — auth is enforced upstream by the API Gateway
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            // Disable default login form
            .formLogin(form -> form.disable())
            // Disable HTTP Basic authentication
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
