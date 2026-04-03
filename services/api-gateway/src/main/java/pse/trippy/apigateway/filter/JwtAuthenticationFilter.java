package pse.trippy.apigateway.filter;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Global filter that validates JWT tokens on protected routes
 * and injects user claim headers for downstream services.
 *
 * <p>Public routes (auth, JWKS, health, actuator) are skipped.
 * On failure the filter returns 401 immediately.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/**",
            "/.well-known/**",
            "/health",
            "/actuator/**"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final String jwksUrl;
    private final AtomicReference<JWKSet> cachedJwkSet = new AtomicReference<>();

    public JwtAuthenticationFilter(
            @Value("${trippy.gateway.jwks-url:http://localhost:8081/.well-known/jwks.json}") String jwksUrl) {
        this.jwksUrl = jwksUrl;
    }

    @Override
    public int getOrder() {
        // Run after correlation-id filter but before routing
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Skip public routes
        for (String pattern : PUBLIC_PATHS) {
            if (pathMatcher.match(pattern, path)) {
                return chain.filter(exchange);
            }
        }

        // Extract Authorization header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            RSAPublicKey publicKey = fetchPublicKey(signedJWT.getHeader().getKeyID());
            if (publicKey == null) {
                log.warn("No matching public key found for kid={}", signedJWT.getHeader().getKeyID());
                return unauthorized(exchange);
            }

            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                log.warn("JWT signature verification failed");
                return unauthorized(exchange);
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Check expiry
            if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new Date())) {
                log.debug("JWT token expired");
                return unauthorized(exchange);
            }

            // Inject user headers for downstream services
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Email", getClaimAsString(claims, "email"))
                    .header("X-User-Role", getClaimAsString(claims, "role"))
                    .header("X-User-Plan", getClaimAsString(claims, "plan"))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception ex) {
            log.warn("JWT processing failed: {}", ex.getMessage());
            return unauthorized(exchange);
        }
    }

    private RSAPublicKey fetchPublicKey(String kid) {
        try {
            JWKSet jwkSet = cachedJwkSet.get();
            if (jwkSet == null) {
                jwkSet = JWKSet.load(new URL(jwksUrl));
                cachedJwkSet.set(jwkSet);
            }

            JWK jwk = jwkSet.getKeyByKeyId(kid);
            if (jwk == null) {
                // Key rotation — refetch once
                jwkSet = JWKSet.load(new URL(jwksUrl));
                cachedJwkSet.set(jwkSet);
                jwk = jwkSet.getKeyByKeyId(kid);
            }

            if (jwk instanceof RSAKey rsaKey) {
                return rsaKey.toRSAPublicKey();
            }
            return null;
        } catch (Exception ex) {
            log.error("Failed to fetch JWKS from {}: {}", jwksUrl, ex.getMessage());
            return null;
        }
    }

    private String getClaimAsString(JWTClaimsSet claims, String name) {
        try {
            Object value = claims.getClaim(name);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
