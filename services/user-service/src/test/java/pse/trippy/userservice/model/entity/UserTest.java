package pse.trippy.userservice.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link User} entity.
 *
 * <p>Validates field defaults, builder behaviour, and JPA lifecycle callbacks.
 * No Spring context is loaded — these are plain JUnit 5 tests.
 */
@DisplayName("User entity")
class UserTest {

    // -------------------------------------------------------------------------
    // Builder helpers
    // -------------------------------------------------------------------------

    private User.UserBuilder validUserBuilder() {
        return User.builder()
                .email("alice@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .displayName("Alice");
    }

    // =========================================================================
    // Default values
    // =========================================================================

    @Nested
    @DisplayName("Default field values")
    class DefaultValues {

        @Test
        @DisplayName("role defaults to USER when not set")
        void roleDefaultsToUser() {
            User user = validUserBuilder().build();
            assertThat(user.getRole()).isEqualTo(UserRole.USER);
        }

        @Test
        @DisplayName("plan defaults to FREE when not set")
        void planDefaultsToFree() {
            User user = validUserBuilder().build();
            assertThat(user.getPlan()).isEqualTo(SubscriptionPlan.FREE);
        }

        @Test
        @DisplayName("emailVerified defaults to false when not set")
        void emailVerifiedDefaultsFalse() {
            User user = validUserBuilder().build();
            assertThat(user.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("optional fields default to null")
        void optionalFieldsDefaultToNull() {
            User user = validUserBuilder().build();
            assertThat(user.getBio()).isNull();
            assertThat(user.getPhoneNumber()).isNull();
            assertThat(user.getAvatarUrl()).isNull();
        }
    }

    // =========================================================================
    // Builder with explicit values
    // =========================================================================

    @Nested
    @DisplayName("Builder with explicit values")
    class ExplicitValues {

        @Test
        @DisplayName("builds user with all fields set correctly")
        void buildsFullUser() {
            User user = validUserBuilder()
                    .bio("Travel enthusiast")
                    .phoneNumber("+491234567890")
                    .avatarUrl("https://cdn.trippy.dev/avatars/alice.png")
                    .role(UserRole.ADMIN)
                    .plan(SubscriptionPlan.PREMIUM)
                    .emailVerified(true)
                    .build();

            assertThat(user.getEmail()).isEqualTo("alice@example.com");
            assertThat(user.getDisplayName()).isEqualTo("Alice");
            assertThat(user.getBio()).isEqualTo("Travel enthusiast");
            assertThat(user.getPhoneNumber()).isEqualTo("+491234567890");
            assertThat(user.getAvatarUrl()).isEqualTo("https://cdn.trippy.dev/avatars/alice.png");
            assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
            assertThat(user.getPlan()).isEqualTo(SubscriptionPlan.PREMIUM);
            assertThat(user.isEmailVerified()).isTrue();
        }
    }

    // =========================================================================
    // JPA lifecycle callbacks
    // =========================================================================

    @Nested
    @DisplayName("JPA lifecycle callbacks")
    class LifecycleCallbacks {

        @Test
        @DisplayName("prePersist sets createdAt and updatedAt")
        void prePersistSetsTimestamps() {
            User user = validUserBuilder().build();
            Instant before = Instant.now();
            user.prePersist();
            Instant after = Instant.now();

            assertThat(user.getCreatedAt()).isNotNull().isBetween(before, after);
            assertThat(user.getUpdatedAt()).isNotNull().isBetween(before, after);
        }

        @Test
        @DisplayName("prePersist sets createdAt equal to updatedAt")
        void prePersistTimestampsAreEqual() {
            User user = validUserBuilder().build();
            user.prePersist();
            assertThat(user.getCreatedAt()).isEqualTo(user.getUpdatedAt());
        }

        @Test
        @DisplayName("preUpdate refreshes updatedAt without changing createdAt")
        void preUpdateOnlyChangesUpdatedAt() throws InterruptedException {
            User user = validUserBuilder().build();
            user.prePersist();
            Instant originalCreatedAt = user.getCreatedAt();

            // Small delay to ensure the updated timestamp is strictly after created
            Thread.sleep(5);
            user.preUpdate();

            assertThat(user.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(user.getUpdatedAt()).isAfter(originalCreatedAt);
        }
    }

    // =========================================================================
    // Setter usage
    // =========================================================================

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("email can be updated via setter")
        void setEmailWorks() {
            User user = validUserBuilder().build();
            user.setEmail("newemail@example.com");
            assertThat(user.getEmail()).isEqualTo("newemail@example.com");
        }

        @Test
        @DisplayName("emailVerified can be set to true via setter")
        void setEmailVerifiedWorks() {
            User user = validUserBuilder().build();
            user.setEmailVerified(true);
            assertThat(user.isEmailVerified()).isTrue();
        }
    }
}
