package pse.trippy.userservice.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pse.trippy.userservice.config.RsaKeyProperties;
import pse.trippy.userservice.model.entity.User;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtService}.
 *
 * <p>Uses a real RSA key pair (generated in-memory) — no Spring context required.
 */
@DisplayName("JwtService")
class JwtServiceTest {

    private static final int EXPIRY_SECONDS = 900;

    private JwtService jwtService;
    private RsaKeyProperties rsaKeys;

    @BeforeEach
    void setUp() {
        rsaKeys = new RsaKeyProperties();
        rsaKeys.init();
        jwtService = new JwtService(rsaKeys, EXPIRY_SECONDS);
    }

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .passwordHash("$2a$12$hash")
                .displayName("Alice")
                .role(UserRole.USER)
                .plan(SubscriptionPlan.FREE)
                .emailVerified(true)
                .build();
    }

    // =========================================================================
    // generateAccessToken
    // =========================================================================

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("returns a non-blank serialised JWT")
        void returnsNonBlankToken() {
            String token = jwtService.generateAccessToken(buildUser());
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("token contains the correct subject (user ID)")
        void containsSubjectClaim() throws ParseException {
            User user = buildUser();
            String token = jwtService.generateAccessToken(user);

            JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
            assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        }

        @Test
        @DisplayName("token contains email, displayName, role, plan, emailVerified claims")
        void containsAllRequiredClaims() throws ParseException {
            User user = buildUser();
            String token = jwtService.generateAccessToken(user);

            JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();

            assertThat(claims.getStringClaim("email")).isEqualTo("alice@example.com");
            assertThat(claims.getStringClaim("displayName")).isEqualTo("Alice");
            assertThat(claims.getStringClaim("role")).isEqualTo("USER");
            assertThat(claims.getStringClaim("plan")).isEqualTo("FREE");
            assertThat(claims.getBooleanClaim("emailVerified")).isTrue();
        }

        @Test
        @DisplayName("token contains a unique jti")
        void containsJti() throws ParseException {
            User user = buildUser();
            String token = jwtService.generateAccessToken(user);

            JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
            assertThat(claims.getJWTID()).isNotBlank();
        }

        @Test
        @DisplayName("token expires approximately at the configured duration")
        void expiresAtConfiguredDuration() throws ParseException {
            User user = buildUser();
            String token = jwtService.generateAccessToken(user);

            JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            Date issuedAt = claims.getIssueTime();

            long diffSeconds = (expiration.getTime() - issuedAt.getTime()) / 1000;
            assertThat(diffSeconds).isEqualTo(EXPIRY_SECONDS);
        }

        @Test
        @DisplayName("header specifies RS256 algorithm and correct kid")
        void headerContainsAlgorithmAndKid() throws ParseException {
            String token = jwtService.generateAccessToken(buildUser());
            SignedJWT signedJwt = SignedJWT.parse(token);

            assertThat(signedJwt.getHeader().getAlgorithm().getName()).isEqualTo("RS256");
            assertThat(signedJwt.getHeader().getKeyID()).isEqualTo(RsaKeyProperties.KEY_ID);
        }

        @Test
        @DisplayName("two generated tokens have different jti values")
        void differentTokensHaveDifferentJti() throws ParseException {
            User user = buildUser();
            String token1 = jwtService.generateAccessToken(user);
            String token2 = jwtService.generateAccessToken(user);

            String jti1 = SignedJWT.parse(token1).getJWTClaimsSet().getJWTID();
            String jti2 = SignedJWT.parse(token2).getJWTClaimsSet().getJWTID();

            assertThat(jti1).isNotEqualTo(jti2);
        }
    }

    // =========================================================================
    // getAccessTokenExpirySeconds
    // =========================================================================

    @Nested
    @DisplayName("getAccessTokenExpirySeconds")
    class GetAccessTokenExpirySeconds {

        @Test
        @DisplayName("returns the configured value")
        void returnsConfiguredExpiry() {
            assertThat(jwtService.getAccessTokenExpirySeconds()).isEqualTo(EXPIRY_SECONDS);
        }
    }
}
