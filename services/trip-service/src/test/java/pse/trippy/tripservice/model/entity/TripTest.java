package pse.trippy.tripservice.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pse.trippy.tripservice.model.enums.TripStatus;
import pse.trippy.tripservice.model.enums.TripVisibility;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link Trip} entity.
 *
 * <p>Validates field defaults, builder behaviour, and JPA lifecycle callbacks.
 * No Spring context is loaded — these are plain JUnit 5 tests.
 */
@DisplayName("Trip entity")
class TripTest {

    private static final UUID CREATOR_ID = UUID.randomUUID();

    private Trip.TripBuilder validTripBuilder() {
        return Trip.builder()
                .title("Europe Backpacking")
                .destination("Europe")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 20))
                .createdBy(CREATOR_ID);
    }

    // =========================================================================
    // Default values
    // =========================================================================

    @Nested
    @DisplayName("Default field values")
    class DefaultValues {

        @Test
        @DisplayName("status defaults to DRAFT when not set")
        void statusDefaultsToDraft() {
            Trip trip = validTripBuilder().build();
            assertThat(trip.getStatus()).isEqualTo(TripStatus.DRAFT);
        }

        @Test
        @DisplayName("visibility defaults to PRIVATE when not set")
        void visibilityDefaultsToPrivate() {
            Trip trip = validTripBuilder().build();
            assertThat(trip.getVisibility()).isEqualTo(TripVisibility.PRIVATE);
        }

        @Test
        @DisplayName("maxParticipants defaults to 10 when not set")
        void maxParticipantsDefaultsTen() {
            Trip trip = validTripBuilder().build();
            assertThat(trip.getMaxParticipants()).isEqualTo(10);
        }

        @Test
        @DisplayName("optional fields default to null")
        void optionalFieldsDefaultToNull() {
            Trip trip = validTripBuilder().build();
            assertThat(trip.getDescription()).isNull();
            assertThat(trip.getCoverImageUrl()).isNull();
            assertThat(trip.getId()).isNull();
        }
    }

    // =========================================================================
    // Builder with explicit values
    // =========================================================================

    @Nested
    @DisplayName("Builder with explicit values")
    class ExplicitValues {

        @Test
        @DisplayName("builds trip with all fields set correctly")
        void buildsFullTrip() {
            Trip trip = validTripBuilder()
                    .description("A 20-day adventure across Europe")
                    .status(TripStatus.PLANNED)
                    .visibility(TripVisibility.PUBLIC)
                    .maxParticipants(5)
                    .coverImageUrl("https://cdn.trippy.dev/covers/europe.jpg")
                    .build();

            assertThat(trip.getTitle()).isEqualTo("Europe Backpacking");
            assertThat(trip.getDestination()).isEqualTo("Europe");
            assertThat(trip.getDescription()).isEqualTo("A 20-day adventure across Europe");
            assertThat(trip.getStatus()).isEqualTo(TripStatus.PLANNED);
            assertThat(trip.getVisibility()).isEqualTo(TripVisibility.PUBLIC);
            assertThat(trip.getMaxParticipants()).isEqualTo(5);
            assertThat(trip.getCoverImageUrl()).isEqualTo("https://cdn.trippy.dev/covers/europe.jpg");
            assertThat(trip.getCreatedBy()).isEqualTo(CREATOR_ID);
        }
    }

    // =========================================================================
    // JPA lifecycle callbacks
    // =========================================================================

    @Nested
    @DisplayName("JPA lifecycle callbacks")
    class LifecycleCallbacks {

        @Test
        @DisplayName("prePersist sets createdAt and updatedAt to the same instant")
        void prePersistSetsTimestamps() {
            Trip trip = validTripBuilder().build();
            Instant before = Instant.now();
            trip.prePersist();
            Instant after = Instant.now();

            assertThat(trip.getCreatedAt()).isNotNull().isBetween(before, after);
            assertThat(trip.getUpdatedAt()).isNotNull().isBetween(before, after);
            assertThat(trip.getCreatedAt()).isEqualTo(trip.getUpdatedAt());
        }

        @Test
        @DisplayName("preUpdate refreshes updatedAt but leaves createdAt unchanged")
        void preUpdateRefreshesUpdatedAt() throws InterruptedException {
            Trip trip = validTripBuilder().build();
            trip.prePersist();
            Instant originalCreatedAt = trip.getCreatedAt();

            Thread.sleep(5);
            trip.preUpdate();

            assertThat(trip.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(trip.getUpdatedAt()).isAfter(originalCreatedAt);
        }

        @Test
        @DisplayName("direct setter overrides prePersist timestamps")
        void directSetterOverridesTimestamps() {
            Trip trip = validTripBuilder().build();
            Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
            trip.setCreatedAt(fixed);
            trip.setUpdatedAt(fixed);

            assertThat(trip.getCreatedAt()).isEqualTo(fixed);
            assertThat(trip.getUpdatedAt()).isEqualTo(fixed);
        }
    }
}
