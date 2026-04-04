package pse.trippy.apigateway.filter;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import pse.trippy.apigateway.service.JwksClient;
import reactor.core.publisher.Mono;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Global gateway filter that validates JWT Bearer tokens on all protected routes.
 *
 * <p>Public routes ({@code /auth/**}, {@code /.well-known/**}, {@code /health}) pass
 * through without a token. For every other path the filter:
 * <ol>
 *   <li>Requires a {@code Authorization: Bearer <token>} header.</li>
 *   <li>Validates the RS256 signature using the cached JWKS public key.</li>
 *   <li>Verifies the token has not expired.</li>
 *   <li>Injects {@code X-User-Id}, {@code X-User-Role}, {@code X-User-Email},
 *       {@code X-User-Plan} headers for downstream services.</li>
 *   <li>Strips the {@code Authorization} header before forwarding.</li>
 * </ol>
 * Runs at order {@code -1} so it executes before all other global filters.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final int FILTER_ORDER = -1;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/**",
            "/.well-known/**",
            "/health",
            "/actuator/health"
    );

    private final JwksClient jwksClient;

    @Override
    public int getOrder() {
        return FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            JWTClaimsSet claims = validateToken(token);
            if (claims == null) {
                return unauthorized(exchange);
            }

            String userId = claims.getSubject();
            String role = (String) claims.getClaim("role");
            String email = (String) claims.getClaim("email");
            String plan = (String) claims.getClaim("plan");

            if (userId == null || role == null || email == null || plan == null) {
                log.debug("JWT missing required claims (sub, role, email, plan)");
                return unauthorized(exchange);
            }

            ServerWebExchange mutatedExchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Role", role)
                            .header("X-User-Email", email)
                            .header("X-User-Plan", plan)
                            .headers(h -> h.remove(HttpHeaders.AUTHORIZATION))
                            .build())
                    .build();

            return chain.filter(mutatedExchange);

        } catch (ParseException | JOSEException ex) {
            log.debug("JWT validation error: {}", ex.getMessage());
            return unauthorized(exchange);
        }
    }

    /**
     * Validates the JWT signature and expiration.
     * On first signature failure, refreshes JWKS and retries once.
     *
     * @param token the raw JWT string
     * @return verified claims, or {@code null} if validation fails
     */
    private JWTClaimsSet validateToken(String token) throws ParseException, JOSEException {
        RSAPublicKey key = jwksClient.getPublicKey();
        if (key == null) {
            log.warn("No JWKS public key available — rejecting token");
            return null;
        }

        SignedJWT jwt = SignedJWT.parse(token);
        boolean valid = jwt.verify(new RSASSAVerifier(key));

        if (!valid) {
            // Refresh JWKS and retry with the new key
            RSAPublicKey refreshedKey = jwksClient.refreshAndGet();
            if (refreshedKey != null) {
                jwt = SignedJWT.parse(token);
                valid = jwt.verify(new RSASSAVerifier(refreshedKey));
            }
        }

        if (!valid) {
            return null;
        }

        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        Date exp = claims.getExpirationTime();
        if (exp == null || exp.before(new Date())) {
            log.debug("JWT expired at {}", exp);
            return null;
        }

        return claims;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(p -> PATH_MATCHER.match(p, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
