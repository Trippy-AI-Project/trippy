package pse.trippy.tripservice.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link DayPlan} entity.
 */
@DisplayName("DayPlan entity")
class DayPlanTest {

    private Trip buildTrip() {
        return Trip.builder()
                .title("Test Trip")
                .destination("Rome")
                .startDate(LocalDate.of(2026, 9, 1))
                .endDate(LocalDate.of(2026, 9, 5))
                .createdBy(UUID.randomUUID())
                .build();
    }

    private Itinerary buildItinerary() {
        return Itinerary.builder().trip(buildTrip()).build();
    }

    @Nested
    @DisplayName("Builder behaviour")
    class BuilderBehaviour {

        @Test
        @DisplayName("builds day plan with required fields")
        void buildsDayPlanWithRequiredFields() {
            Itinerary itinerary = buildItinerary();
            DayPlan dayPlan = DayPlan.builder()
                    .itinerary(itinerary)
                    .dayNumber(1)
                    .build();

            assertThat(dayPlan.getItinerary()).isSameAs(itinerary);
            assertThat(dayPlan.getDayNumber()).isEqualTo(1);
            assertThat(dayPlan.getDate()).isNull();
            assertThat(dayPlan.getTitle()).isNull();
        }

        @Test
        @DisplayName("builds day plan with all optional fields")
        void buildsDayPlanWithAllFields() {
            LocalDate date = LocalDate.of(2026, 9, 1);
            DayPlan dayPlan = DayPlan.builder()
                    .itinerary(buildItinerary())
                    .dayNumber(3)
                    .date(date)
                    .title("Colosseum & Forum")
                    .build();

            assertThat(dayPlan.getDayNumber()).isEqualTo(3);
            assertThat(dayPlan.getDate()).isEqualTo(date);
            assertThat(dayPlan.getTitle()).isEqualTo("Colosseum & Forum");
        }
    }
}
