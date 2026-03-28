package pse.trippy.userservice.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link RefreshToken} entity.
 *
 * <p>Covers default construction, lifecycle callbacks, and the {@code isExpired()} helper.
 * No Spring context is loaded — plain JUnit 5 tests.
 */
@DisplayName("RefreshToken entity")
class RefreshTokenTest {

    // -------------------------------------------------------------------------
    // Builder helpers
    // -------------------------------------------------------------------------

    private User buildUser() {
        return User.builder()
                .email("bob@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .displayName("Bob")
                .build();
    }

    private RefreshToken.RefreshTokenBuilder validTokenBuilder() {
        return RefreshToken.builder()
                .token("hashed-token-value")
                .user(buildUser())
                .expiresAt(Instant.now().plusSeconds(604800)); // 7 days
    }

    // =========================================================================
    // Builder
    // =========================================================================

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("builds token with all specified fields")
        void buildsWithAllFields() {
            Instant expiry = Instant.now().plusSeconds(604800);
            User user = buildUser();

            RefreshToken token = RefreshToken.builder()
                    .token("hashed-value")
                    .user(user)
                    .expiresAt(expiry)
                    .build();

            assertThat(token.getToken()).isEqualTo("hashed-value");
            assertThat(token.getUser()).isSameAs(user);
            assertThat(token.getExpiresAt()).isEqualTo(expiry);
        }

        @Test
        @DisplayName("id is null before it is persisted by JPA")
        void idIsNullBeforePersist() {
            RefreshToken token = validTokenBuilder().build();
            assertThat(token.getId()).isNull();
        }

        @Test
        @DisplayName("createdAt is null before prePersist is called")
        void createdAtNullBeforePrePersist() {
            RefreshToken token = validTokenBuilder().build();
            assertThat(token.getCreatedAt()).isNull();
        }
    }

    // =========================================================================
    // JPA lifecycle callbacks
    // =========================================================================

    @Nested
    @DisplayName("JPA lifecycle callbacks")
    class LifecycleCallbacks {

        @Test
        @DisplayName("prePersist sets createdAt to a recent instant")
        void prePersistSetsCreatedAt() {
            RefreshToken token = validTokenBuilder().build();
            Instant before = Instant.now();
            token.prePersist();
            Instant after = Instant.now();

            assertThat(token.getCreatedAt()).isBetween(before, after);
        }

        @Test
        @DisplayName("prePersist does not overwrite expiresAt")
        void prePersistDoesNotChangeExpiresAt() {
            Instant expiry = Instant.now().plusSeconds(604800);
            RefreshToken token = validTokenBuilder().expiresAt(expiry).build();
            token.prePersist();

            assertThat(token.getExpiresAt()).isEqualTo(expiry);
        }
    }

    // =========================================================================
    // isExpired()
    // =========================================================================

    @Nested
    @DisplayName("isExpired()")
    class IsExpired {

        @Test
        @DisplayName("returns false for a token that expires in the future")
        void returnsFalseForFutureExpiry() {
            RefreshToken token = validTokenBuilder()
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            assertThat(token.isExpired()).isFalse();
        }

        @Test
        @DisplayName("returns true for a token that expired in the past")
        void returnsTrueForPastExpiry() {
            RefreshToken token = validTokenBuilder()
                    .expiresAt(Instant.now().minusSeconds(1))
                    .build();

            assertThat(token.isExpired()).isTrue();
        }
    }
}
