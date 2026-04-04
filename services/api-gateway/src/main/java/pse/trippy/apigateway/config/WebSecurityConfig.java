package pse.trippy.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security configuration for the API Gateway.
 *
 * <p>Disables form login, HTTP Basic, and CSRF — the gateway delegates all
 * authentication and authorisation to {@link pse.trippy.apigateway.filter.JwtAuthenticationFilter}.
 * Spring Security is configured to permit every exchange so it does not
 * interfere with the filter's 401 responses.
 *
 * <p>{@link EnableScheduling} is declared here to activate the periodic
 * JWKS cache refresh in {@link pse.trippy.apigateway.service.JwksClient}.
 */
@Configuration
@EnableWebFluxSecurity
@EnableScheduling
public class WebSecurityConfig {

    /**
     * Builds a permissive security chain that disables all default Spring Security
     * mechanisms (form login, basic auth, CSRF). All JWT enforcement is handled by
     * the custom global filter.
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(auth -> auth.anyExchange().permitAll())
                .build();
    }
}
