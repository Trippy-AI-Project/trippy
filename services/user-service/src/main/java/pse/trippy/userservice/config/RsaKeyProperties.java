package pse.trippy.userservice.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Generates an RSA 2048-bit key pair on application startup for RS256 JWT signing.
 *
 * <p>The private key signs access tokens; the public key is exposed via the JWKS endpoint
 * so that the API Gateway (and other consumers) can verify token signatures.
 */
@Component
@Slf4j
@Getter
public class RsaKeyProperties {

    /** Key identifier included in JWTs and the JWKS response. */
    public final String KEY_ID = "trippy-key-" + java.util.UUID.randomUUID().toString();

    private static final int KEY_SIZE = 2048;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    /** Generates the RSA key pair once during bean initialisation. */
    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE);
            KeyPair keyPair = generator.generateKeyPair();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            log.info("RSA key pair generated (kid={})", KEY_ID);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA algorithm not available", ex);
        }
    }
}
