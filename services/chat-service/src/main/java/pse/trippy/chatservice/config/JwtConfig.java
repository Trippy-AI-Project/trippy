package pse.trippy.chatservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Configures a {@link JwtDecoder} that validates RS256 access tokens by
 * fetching the public key from the user-service JWKS endpoint.
 *
 * <p>The JwtDecoder is used by {@link WebSocketAuthChannelInterceptor} to
 * authenticate STOMP CONNECT frames server-side (ticket: WS Auth Hardening).
 */
@Configuration
public class JwtConfig {

    @Value("${trippy.jwt.jwks-uri:http://localhost:8081/.well-known/jwks.json}")
    private String jwksUri;

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
    }
}
