package pse.trippy.userservice.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the {@link UserRole} and {@link SubscriptionPlan} enums.
 */
@DisplayName("User enums")
class UserEnumsTest {

    // =========================================================================
    // UserRole
    // =========================================================================

    @Test
    @DisplayName("UserRole.USER exists and its name is USER")
    void userRoleUserExists() {
        assertThat(UserRole.USER.name()).isEqualTo("USER");
    }

    @Test
    @DisplayName("UserRole.ADMIN exists and its name is ADMIN")
    void userRoleAdminExists() {
        assertThat(UserRole.ADMIN.name()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("UserRole has exactly two values")
    void userRoleHasTwoValues() {
        assertThat(UserRole.values()).containsExactlyInAnyOrder(UserRole.USER, UserRole.ADMIN);
    }

    @Test
    @DisplayName("UserRole.valueOf handles valid string")
    void userRoleValueOfValid() {
        assertThat(UserRole.valueOf("USER")).isEqualTo(UserRole.USER);
        assertThat(UserRole.valueOf("ADMIN")).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("UserRole.valueOf throws for unknown string")
    void userRoleValueOfInvalid() {
        assertThatThrownBy(() -> UserRole.valueOf("SUPERUSER"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // SubscriptionPlan
    // =========================================================================

    @Test
    @DisplayName("SubscriptionPlan has exactly three values")
    void subscriptionPlanHasThreeValues() {
        assertThat(SubscriptionPlan.values())
                .containsExactlyInAnyOrder(
                        SubscriptionPlan.FREE,
                        SubscriptionPlan.PREMIUM,
                        SubscriptionPlan.ENTERPRISE);
    }

    @Test
    @DisplayName("SubscriptionPlan.valueOf handles all valid strings")
    void subscriptionPlanValueOfAllValid() {
        assertThat(SubscriptionPlan.valueOf("FREE")).isEqualTo(SubscriptionPlan.FREE);
        assertThat(SubscriptionPlan.valueOf("PREMIUM")).isEqualTo(SubscriptionPlan.PREMIUM);
        assertThat(SubscriptionPlan.valueOf("ENTERPRISE")).isEqualTo(SubscriptionPlan.ENTERPRISE);
    }

    @Test
    @DisplayName("SubscriptionPlan.valueOf throws for unknown string")
    void subscriptionPlanValueOfInvalid() {
        assertThatThrownBy(() -> SubscriptionPlan.valueOf("BASIC"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
