package pse.trippy.tripservice.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for all trip-service enums:
 * {@link TripStatus}, {@link TripVisibility}, {@link ParticipantRole},
 * {@link ParticipantStatus}, and {@link ActivityCategory}.
 */
@DisplayName("Trip enums")
class TripEnumsTest {

    // =========================================================================
    // TripStatus
    // =========================================================================

    @Test
    @DisplayName("TripStatus has exactly five values")
    void tripStatusHasFiveValues() {
        assertThat(TripStatus.values()).containsExactlyInAnyOrder(
                TripStatus.DRAFT, TripStatus.PLANNED, TripStatus.ONGOING,
                TripStatus.COMPLETED, TripStatus.CANCELLED);
    }

    @Test
    @DisplayName("TripStatus.valueOf returns correct constants")
    void tripStatusValueOf() {
        assertThat(TripStatus.valueOf("DRAFT")).isEqualTo(TripStatus.DRAFT);
        assertThat(TripStatus.valueOf("PLANNED")).isEqualTo(TripStatus.PLANNED);
        assertThat(TripStatus.valueOf("ONGOING")).isEqualTo(TripStatus.ONGOING);
        assertThat(TripStatus.valueOf("COMPLETED")).isEqualTo(TripStatus.COMPLETED);
        assertThat(TripStatus.valueOf("CANCELLED")).isEqualTo(TripStatus.CANCELLED);
    }

    @Test
    @DisplayName("TripStatus.valueOf throws for unknown string")
    void tripStatusValueOfInvalid() {
        assertThatThrownBy(() -> TripStatus.valueOf("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // =========================================================================
    // TripVisibility
    // =========================================================================

    @Test
    @DisplayName("TripVisibility has exactly three values")
    void tripVisibilityHasThreeValues() {
        assertThat(TripVisibility.values()).containsExactlyInAnyOrder(
                TripVisibility.PRIVATE, TripVisibility.PUBLIC, TripVisibility.UNLISTED);
    }

    @Test
    @DisplayName("TripVisibility.valueOf returns correct constants")
    void tripVisibilityValueOf() {
        assertThat(TripVisibility.valueOf("PRIVATE")).isEqualTo(TripVisibility.PRIVATE);
        assertThat(TripVisibility.valueOf("PUBLIC")).isEqualTo(TripVisibility.PUBLIC);
        assertThat(TripVisibility.valueOf("UNLISTED")).isEqualTo(TripVisibility.UNLISTED);
    }

    // =========================================================================
    // ParticipantRole
    // =========================================================================

    @Test
    @DisplayName("ParticipantRole has exactly three values")
    void participantRoleHasThreeValues() {
        assertThat(ParticipantRole.values()).containsExactlyInAnyOrder(
                ParticipantRole.OWNER, ParticipantRole.EDITOR, ParticipantRole.VIEWER);
    }

    @Test
    @DisplayName("ParticipantRole.valueOf returns correct constants")
    void participantRoleValueOf() {
        assertThat(ParticipantRole.valueOf("OWNER")).isEqualTo(ParticipantRole.OWNER);
        assertThat(ParticipantRole.valueOf("EDITOR")).isEqualTo(ParticipantRole.EDITOR);
        assertThat(ParticipantRole.valueOf("VIEWER")).isEqualTo(ParticipantRole.VIEWER);
    }

    // =========================================================================
    // ParticipantStatus
    // =========================================================================

    @Test
    @DisplayName("ParticipantStatus has exactly four values")
    void participantStatusHasFourValues() {
        assertThat(ParticipantStatus.values()).containsExactlyInAnyOrder(
                ParticipantStatus.PENDING, ParticipantStatus.ACCEPTED,
                ParticipantStatus.DECLINED, ParticipantStatus.LEFT);
    }

    @Test
    @DisplayName("ParticipantStatus.valueOf returns correct constants")
    void participantStatusValueOf() {
        assertThat(ParticipantStatus.valueOf("PENDING")).isEqualTo(ParticipantStatus.PENDING);
        assertThat(ParticipantStatus.valueOf("ACCEPTED")).isEqualTo(ParticipantStatus.ACCEPTED);
        assertThat(ParticipantStatus.valueOf("DECLINED")).isEqualTo(ParticipantStatus.DECLINED);
        assertThat(ParticipantStatus.valueOf("LEFT")).isEqualTo(ParticipantStatus.LEFT);
    }

    // =========================================================================
    // ActivityCategory
    // =========================================================================

    @Test
    @DisplayName("ActivityCategory has exactly seven values")
    void activityCategoryHasSevenValues() {
        assertThat(ActivityCategory.values()).containsExactlyInAnyOrder(
                ActivityCategory.ACCOMMODATION, ActivityCategory.TRANSPORT,
                ActivityCategory.FOOD, ActivityCategory.SIGHTSEEING,
                ActivityCategory.ACTIVITY, ActivityCategory.SHOPPING,
                ActivityCategory.OTHER);
    }

    @Test
    @DisplayName("ActivityCategory.valueOf returns correct constants")
    void activityCategoryValueOf() {
        assertThat(ActivityCategory.valueOf("FOOD")).isEqualTo(ActivityCategory.FOOD);
        assertThat(ActivityCategory.valueOf("OTHER")).isEqualTo(ActivityCategory.OTHER);
    }

    @Test
    @DisplayName("ActivityCategory.valueOf throws for unknown string")
    void activityCategoryValueOfInvalid() {
        assertThatThrownBy(() -> ActivityCategory.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
