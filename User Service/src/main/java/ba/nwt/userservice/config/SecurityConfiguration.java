package ba.nwt.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;

/**
 * Spring Security Configuration
 * Configures authentication and authorization rules
 */
@Configuration
@EnableWebSecurity
@Order(0)
public class SecurityConfiguration {

    /**
     * Configure security filter chain
     * - Allow public access to authentication endpoints
     * - Allow other endpoints (resources are protected in API Gateway)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for API
            .csrf(csrf -> csrf.disable())
            // Set session policy to stateless (JWT doesn't use sessions)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                // All other requests require authentication (but API Gateway handles JWT validation)
                .anyRequest().authenticated()
            )
            // Disable default login form
            .formLogin(form -> form.disable())
            // Disable HTTP Basic authentication
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}
