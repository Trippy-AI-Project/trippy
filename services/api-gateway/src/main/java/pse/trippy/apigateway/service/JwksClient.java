package pse.trippy.apigateway.service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fetches and caches the RSA public key from the user-service JWKS endpoint.
 *
 * <p>The key is loaded once at startup and refreshed every 5 minutes via
 * {@link #refreshKeys()}. The filter can also trigger a refresh on demand
 * via {@link #refreshAndGet()} when a signature verification fails.
 */
@Component
@Slf4j
public class JwksClient {

    private final WebClient webClient;
    private final String jwksUri;
    private final AtomicReference<RSAPublicKey> publicKeyRef = new AtomicReference<>();

    public JwksClient(
            WebClient.Builder webClientBuilder,
            @Value("${trippy.gateway.jwks-uri}") String jwksUri) {
        this.webClient = webClientBuilder.build();
        this.jwksUri = jwksUri;
    }

    /**
     * Loads the JWKS public key at startup, retrying up to 5 times with 3-second
     * delays to tolerate user-service starting after the gateway.
     */
    @PostConstruct
    public void init() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            refreshKeys();
            if (publicKeyRef.get() != null) {
                return;
            }
            log.warn("JWKS not yet available (attempt {}/5), retrying in 3 s...", attempt);
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        log.warn("Could not load JWKS at startup — all tokens will be rejected until the scheduled refresh succeeds");
    }

    /**
     * Returns the cached RSA public key, or {@code null} if not yet loaded.
     */
    public RSAPublicKey getPublicKey() {
        return publicKeyRef.get();
    }

    /**
     * Forces an immediate JWKS refresh and returns the updated public key.
     */
    public RSAPublicKey refreshAndGet() {
        refreshKeys();
        return publicKeyRef.get();
    }

    /**
     * Scheduled JWKS cache refresh every 30 s — fast enough to self-heal when
     * user-service starts after the gateway, and picks up key rotations quickly.
     */
    @Scheduled(fixedDelay = 30_000)
    public void refreshKeys() {
        try {
            String jwksJson = webClient.get()
                    .uri(jwksUri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            JWKSet jwkSet = JWKSet.parse(jwksJson);
            RSAKey rsaKey = jwkSet.getKeys().stream()
                    .filter(RSAKey.class::isInstance)
                    .map(RSAKey.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No RSA key found in JWKS response"));

            publicKeyRef.set(rsaKey.toRSAPublicKey());
            log.info("JWKS refreshed from {}", jwksUri);
        } catch (Exception ex) {
            log.warn("Failed to refresh JWKS from {}: {}", jwksUri, ex.getMessage());
        }
    }
}
