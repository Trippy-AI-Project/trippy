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
     * Loads the JWKS public key at startup.
     * Failure is logged but does not prevent gateway startup.
     */
    @PostConstruct
    public void init() {
        refreshKeys();
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
     * Scheduled JWKS cache refresh every 5 minutes.
     */
    @Scheduled(fixedDelay = 300_000)
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
