package pse.trippy.tripservice.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link Itinerary} entity.
 */
@DisplayName("Itinerary entity")
class ItineraryTest {

    private Trip buildTrip() {
        return Trip.builder()
                .title("Test Trip")
                .destination("Paris")
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 7))
                .createdBy(UUID.randomUUID())
                .build();
    }

    @Nested
    @DisplayName("Default field values")
    class DefaultValues {

        @Test
        @DisplayName("timestamps default to null before persist")
        void timestampsAreNullBeforePersist() {
            Itinerary itinerary = Itinerary.builder().trip(buildTrip()).build();
            assertThat(itinerary.getCreatedAt()).isNull();
            assertThat(itinerary.getUpdatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("JPA lifecycle callbacks")
    class LifecycleCallbacks {

        @Test
        @DisplayName("prePersist sets createdAt and updatedAt to the same instant")
        void prePersistSetsTimestamps() {
            Itinerary itinerary = Itinerary.builder().trip(buildTrip()).build();
            Instant before = Instant.now();
            itinerary.prePersist();
            Instant after = Instant.now();

            assertThat(itinerary.getCreatedAt()).isNotNull().isBetween(before, after);
            assertThat(itinerary.getUpdatedAt()).isNotNull().isBetween(before, after);
            assertThat(itinerary.getCreatedAt()).isEqualTo(itinerary.getUpdatedAt());
        }

        @Test
        @DisplayName("preUpdate refreshes updatedAt but leaves createdAt unchanged")
        void preUpdateRefreshesUpdatedAt() throws InterruptedException {
            Itinerary itinerary = Itinerary.builder().trip(buildTrip()).build();
            itinerary.prePersist();
            Instant originalCreatedAt = itinerary.getCreatedAt();

            Thread.sleep(5);
            itinerary.preUpdate();

            assertThat(itinerary.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(itinerary.getUpdatedAt()).isAfter(originalCreatedAt);
        }
    }
}
