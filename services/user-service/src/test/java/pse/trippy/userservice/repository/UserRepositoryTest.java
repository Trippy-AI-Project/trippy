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
import pse.trippy.userservice.model.entity.User;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link UserRepository} using an H2 in-memory database.
 *
 * <p>{@code @DataJpaTest} loads only the JPA layer (entities, repositories, datasource).
 * Each test runs in a transaction that is rolled back on completion.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("test")
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = userRepository.save(User.builder()
                .email("charlie@example.com")
                .passwordHash("mock-pw-hash")
                .displayName("Charlie")
                .role(UserRole.USER)
                .plan(SubscriptionPlan.FREE)
                .emailVerified(false)
                .build());
    }

    // =========================================================================
    // findByEmail
    // =========================================================================

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("returns user when email exists")
        void returnsUserForExistingEmail() {
            Optional<User> result = userRepository.findByEmail("charlie@example.com");
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("charlie@example.com");
        }

        @Test
        @DisplayName("returns empty when email does not exist")
        void returnsEmptyForUnknownEmail() {
            Optional<User> result = userRepository.findByEmail("nobody@example.com");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("lookup is case-sensitive")
        void caseSensitiveLookup() {
            Optional<User> result = userRepository.findByEmail("CHARLIE@EXAMPLE.COM");
            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // existsByEmail
    // =========================================================================

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("returns true when email is registered")
        void returnsTrueForRegisteredEmail() {
            assertThat(userRepository.existsByEmail("charlie@example.com")).isTrue();
        }

        @Test
        @DisplayName("returns false when email is not registered")
        void returnsFalseForUnknownEmail() {
            assertThat(userRepository.existsByEmail("ghost@example.com")).isFalse();
        }
    }

    // =========================================================================
    // save / findById
    // =========================================================================

    @Nested
    @DisplayName("save and findById")
    class SaveAndFind {

        @Test
        @DisplayName("persisted user gets an auto-generated UUID")
        void savedUserHasGeneratedId() {
            assertThat(savedUser.getId()).isNotNull();
        }

        @Test
        @DisplayName("timestamps are set by @PrePersist")
        void prePersistSetsTimestamps() {
            assertThat(savedUser.getCreatedAt()).isNotNull();
            assertThat(savedUser.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("default role and plan are persisted correctly")
        void defaultEnumsArePersisted() {
            User fetched = userRepository.findById(savedUser.getId()).orElseThrow();
            assertThat(fetched.getRole()).isEqualTo(UserRole.USER);
            assertThat(fetched.getPlan()).isEqualTo(SubscriptionPlan.FREE);
        }

        @Test
        @DisplayName("optional fields can be updated and re-fetched")
        void optionalFieldsArePersisted() {
            savedUser.setBio("Explorer");
            savedUser.setAvatarUrl("https://cdn.trippy.dev/avatars/charlie.png");
            userRepository.save(savedUser);

            User fetched = userRepository.findById(savedUser.getId()).orElseThrow();
            assertThat(fetched.getBio()).isEqualTo("Explorer");
            assertThat(fetched.getAvatarUrl()).isEqualTo("https://cdn.trippy.dev/avatars/charlie.png");
        }
    }

    // =========================================================================
    // delete
    // =========================================================================

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deleted user can no longer be found by id")
        void deletedUserNotFound() {
            userRepository.delete(savedUser);
            assertThat(userRepository.findById(savedUser.getId())).isEmpty();
        }

        @Test
        @DisplayName("deleted user email becomes available again")
        void deletedEmailNoLongerExists() {
            userRepository.delete(savedUser);
            assertThat(userRepository.existsByEmail("charlie@example.com")).isFalse();
        }
    }
}
