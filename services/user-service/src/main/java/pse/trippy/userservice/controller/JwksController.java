package pse.trippy.userservice.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.userservice.config.RsaKeyProperties;

import java.util.Map;

/**
 * Exposes the JSON Web Key Set (JWKS) endpoint for JWT signature verification.
 *
 * <p>The API Gateway fetches {@code GET /.well-known/jwks.json} on startup
 * (and periodically) to obtain the public key used to validate access tokens.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RsaKeyProperties rsaKeys;

    /**
     * Returns the RSA public key in standard JWKS format (RFC 7517).
     */
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getJwks() {
        RSAKey rsaKey = new RSAKey.Builder(rsaKeys.getPublicKey())
                .keyID(RsaKeyProperties.KEY_ID)
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                .build();

        return new JWKSet(rsaKey).toJSONObject();
    }
}
