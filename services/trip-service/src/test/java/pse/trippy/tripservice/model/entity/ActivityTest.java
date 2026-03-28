package pse.trippy.tripservice.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pse.trippy.tripservice.model.enums.ActivityCategory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link Activity} entity.
 */
@DisplayName("Activity entity")
class ActivityTest {

    private DayPlan buildDayPlan() {
        Trip trip = Trip.builder()
                .title("Test Trip")
                .destination("Barcelona")
                .startDate(LocalDate.of(2026, 10, 1))
                .endDate(LocalDate.of(2026, 10, 5))
                .createdBy(UUID.randomUUID())
                .build();
        Itinerary itinerary = Itinerary.builder().trip(trip).build();
        return DayPlan.builder().itinerary(itinerary).dayNumber(1).build();
    }

    @Nested
    @DisplayName("Default field values")
    class DefaultValues {

        @Test
        @DisplayName("category defaults to OTHER when not set")
        void categoryDefaultsToOther() {
            Activity activity = Activity.builder()
                    .dayPlan(buildDayPlan())
                    .title("Free time")
                    .build();
            assertThat(activity.getCategory()).isEqualTo(ActivityCategory.OTHER);
        }

        @Test
        @DisplayName("orderIndex defaults to 0 when not set")
        void orderIndexDefaultsToZero() {
            Activity activity = Activity.builder()
                    .dayPlan(buildDayPlan())
                    .title("Morning run")
                    .build();
            assertThat(activity.getOrderIndex()).isZero();
        }

        @Test
        @DisplayName("optional fields default to null")
        void optionalFieldsDefaultToNull() {
            Activity activity = Activity.builder()
                    .dayPlan(buildDayPlan())
                    .title("Lunch")
                    .build();
            assertThat(activity.getDescription()).isNull();
            assertThat(activity.getLocation()).isNull();
            assertThat(activity.getStartTime()).isNull();
            assertThat(activity.getEndTime()).isNull();
            assertThat(activity.getNotes()).isNull();
        }
    }

    @Nested
    @DisplayName("Builder with explicit values")
    class ExplicitValues {

        @Test
        @DisplayName("builds activity with all fields set correctly")
        void buildsFullActivity() {
            DayPlan dayPlan = buildDayPlan();
            LocalTime start = LocalTime.of(10, 0);
            LocalTime end = LocalTime.of(12, 0);

            Activity activity = Activity.builder()
                    .dayPlan(dayPlan)
                    .title("Sagrada Familia tour")
                    .description("Guided tour of the iconic Gaudi basilica")
                    .location("Carrer de Mallorca, 401, Barcelona")
                    .startTime(start)
                    .endTime(end)
                    .category(ActivityCategory.SIGHTSEEING)
                    .notes("Book tickets in advance")
                    .orderIndex(2)
                    .build();

            assertThat(activity.getTitle()).isEqualTo("Sagrada Familia tour");
            assertThat(activity.getDescription()).isEqualTo("Guided tour of the iconic Gaudi basilica");
            assertThat(activity.getLocation()).isEqualTo("Carrer de Mallorca, 401, Barcelona");
            assertThat(activity.getStartTime()).isEqualTo(start);
            assertThat(activity.getEndTime()).isEqualTo(end);
            assertThat(activity.getCategory()).isEqualTo(ActivityCategory.SIGHTSEEING);
            assertThat(activity.getNotes()).isEqualTo("Book tickets in advance");
            assertThat(activity.getOrderIndex()).isEqualTo(2);
        }
    }
}
