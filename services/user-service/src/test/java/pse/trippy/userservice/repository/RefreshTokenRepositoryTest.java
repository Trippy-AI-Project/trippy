package pse.trippy.userservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pse.trippy.userservice.model.entity.RefreshToken;
import pse.trippy.userservice.model.entity.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link RefreshTokenRepository} using an H2 in-memory database.
 *
 * <p>Each test runs inside a transaction that is rolled back after completion.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository")
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private RefreshToken activeToken;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("diana@example.com")
                .passwordHash("$2a$12$hashedpw")
                .displayName("Diana")
                .build());

        activeToken = refreshTokenRepository.save(RefreshToken.builder()
                .token("valid-hashed-token")
                .user(user)
                .expiresAt(Instant.now().plusSeconds(604800))
                .build());
    }

    // =========================================================================
    // findByToken
    // =========================================================================

    @Nested
    @DisplayName("findByToken")
    class FindByToken {

        @Test
        @DisplayName("returns token record when token hash exists")
        void returnsTokenForExistingHash() {
            Optional<RefreshToken> result = refreshTokenRepository.findByToken("valid-hashed-token");
            assertThat(result).isPresent();
            assertThat(result.get().getToken()).isEqualTo("valid-hashed-token");
        }

        @Test
        @DisplayName("returns empty for an unknown token hash")
        void returnsEmptyForUnknownToken() {
            Optional<RefreshToken> result = refreshTokenRepository.findByToken("nonexistent-token");
            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // findAllByUserId
    // =========================================================================

    @Nested
    @DisplayName("findAllByUserId")
    class FindAllByUserId {

        @Test
        @DisplayName("returns all tokens for the user")
        void returnsAllTokensForUser() {
            refreshTokenRepository.save(RefreshToken.builder()
                    .token("second-token")
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(2592000))
                    .build());

            List<RefreshToken> tokens = refreshTokenRepository.findAllByUserId(user.getId());
            assertThat(tokens).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when user has no tokens")
        void returnsEmptyListForUserWithNoTokens() {
            User other = userRepository.save(User.builder()
                    .email("eve@example.com")
                    .passwordHash("$2a$12$pw")
                    .displayName("Eve")
                    .build());

            List<RefreshToken> tokens = refreshTokenRepository.findAllByUserId(other.getId());
            assertThat(tokens).isEmpty();
        }
    }

    // =========================================================================
    // deleteAllByUserId
    // =========================================================================

    @Nested
    @DisplayName("deleteAllByUserId")
    class DeleteAllByUserId {

        @Test
        @DisplayName("removes all tokens for the specified user")
        void deletesAllTokensForUser() {
            refreshTokenRepository.deleteAllByUserId(user.getId());
            assertThat(refreshTokenRepository.findAllByUserId(user.getId())).isEmpty();
        }

        @Test
        @DisplayName("does not remove tokens that belong to other users")
        void doesNotAffectOtherUsers() {
            User other = userRepository.save(User.builder()
                    .email("frank@example.com")
                    .passwordHash("$2a$12$pw")
                    .displayName("Frank")
                    .build());
            refreshTokenRepository.save(RefreshToken.builder()
                    .token("frank-token")
                    .user(other)
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .build());

            refreshTokenRepository.deleteAllByUserId(user.getId());

            List<RefreshToken> remaining = refreshTokenRepository.findAllByUserId(other.getId());
            assertThat(remaining).hasSize(1);
        }
    }

    // =========================================================================
    // deleteAllExpiredBefore
    // =========================================================================

    @Nested
    @DisplayName("deleteAllExpiredBefore")
    class DeleteAllExpiredBefore {

        @Test
        @DisplayName("removes tokens whose expiresAt is before the given instant")
        void removesExpiredTokens() {
            refreshTokenRepository.save(RefreshToken.builder()
                    .token("expired-token")
                    .user(user)
                    .expiresAt(Instant.now().minusSeconds(3600))
                    .build());

            refreshTokenRepository.deleteAllExpiredBefore(Instant.now());

            // only the still-valid activeToken should remain
            List<RefreshToken> remaining = refreshTokenRepository.findAllByUserId(user.getId());
            assertThat(remaining).hasSize(1);
            assertThat(remaining.get(0).getToken()).isEqualTo("valid-hashed-token");
        }

        @Test
        @DisplayName("keeps tokens whose expiresAt is in the future")
        void keepsFutureTokens() {
            refreshTokenRepository.deleteAllExpiredBefore(Instant.now());
            assertThat(refreshTokenRepository.findByToken("valid-hashed-token")).isPresent();
        }
    }

    // =========================================================================
    // save
    // =========================================================================

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("persisted token gets an auto-generated UUID")
        void savedTokenHasGeneratedId() {
            assertThat(activeToken.getId()).isNotNull();
        }

        @Test
        @DisplayName("@PrePersist sets createdAt automatically")
        void prePersistSetsCreatedAt() {
            assertThat(activeToken.getCreatedAt()).isNotNull();
        }
    }
}
