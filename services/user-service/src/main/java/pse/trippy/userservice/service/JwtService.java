package pse.trippy.userservice.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pse.trippy.userservice.config.RsaKeyProperties;
import pse.trippy.userservice.model.entity.User;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Service responsible for generating RS256 JWT access tokens.
 *
 * <p>Access tokens contain the claims specified in the API contract:
 * {@code sub}, {@code email}, {@code displayName}, {@code role},
 * {@code plan}, {@code emailVerified}, {@code jti}.
 */
@Service
public class JwtService {

    private final RsaKeyProperties rsaKeys;
    private final int accessTokenExpirySeconds;

    public JwtService(
            RsaKeyProperties rsaKeys,
            @Value("${trippy.jwt.access-token-expiry-seconds}") int accessTokenExpirySeconds) {
        this.rsaKeys = rsaKeys;
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
    }

    /**
     * Generates an RS256-signed JWT access token for the given user.
     *
     * @param user the authenticated user
     * @return the serialised JWT string
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpirySeconds);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("displayName", user.getDisplayName())
                .claim("role", user.getRole().name())
                .claim("plan", user.getPlan().name())
                .claim("emailVerified", user.isEmailVerified())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiry))
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(RsaKeyProperties.KEY_ID)
                .build();

        SignedJWT signedJwt = new SignedJWT(header, claims);

        try {
            JWSSigner signer = new RSASSASigner(rsaKeys.getPrivateKey());
            signedJwt.sign(signer);
        } catch (JOSEException ex) {
            throw new IllegalStateException("Failed to sign JWT", ex);
        }

        return signedJwt.serialize();
    }

    /** Returns the configured access token expiry in seconds. */
    public int getAccessTokenExpirySeconds() {
        return accessTokenExpirySeconds;
    }
}
