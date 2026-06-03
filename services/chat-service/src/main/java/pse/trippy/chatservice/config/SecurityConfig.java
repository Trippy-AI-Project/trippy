package pse.trippy.chatservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the chat-service.
 *
 * <p>HTTP endpoints are protected by the API gateway (which validates JWT and
 * injects X-User-Id/X-User-Role headers). {@link GatewayHeaderAuthFilter}
 * converts those headers into a Spring Security authentication object so that
 * method-level {@code @PreAuthorize} rules and HTTP security constraints work.
 *
 * <p>WebSocket/STOMP authentication is handled inside
 * {@link WebSocketAuthChannelInterceptor} on STOMP CONNECT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;

    public SecurityConfig(GatewayHeaderAuthFilter gatewayHeaderAuthFilter) {
        this.gatewayHeaderAuthFilter = gatewayHeaderAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Moderation endpoints require ADMIN role (enforced via gateway-injected header)
                .requestMatchers("/admin/chat/**").hasRole("ADMIN")
                // All other HTTP endpoints pass through — gateway already validated JWT
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
