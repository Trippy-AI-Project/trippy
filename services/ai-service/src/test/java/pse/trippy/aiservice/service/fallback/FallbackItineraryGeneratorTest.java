package pse.trippy.aiservice.service.fallback;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.request.TripConstraints;
import pse.trippy.aiservice.dto.response.ItineraryResponse;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FallbackItineraryGenerator")
class FallbackItineraryGeneratorTest {

    private FallbackItineraryGenerator generator;

    @BeforeEach
    void setUp() {
        FallbackDestinationCatalogue catalogue = new FallbackDestinationCatalogue(new ObjectMapper());
        generator = new FallbackItineraryGenerator(catalogue);
    }

    @Test
    @DisplayName("generates the exact requested number of days")
    void generatesExactRequestedDayCount() {
        ItineraryResponse response = generator.generate(request("Paris, France", 7, "MODERATE",
                List.of(), List.of()), "AI_PROVIDER_UNAVAILABLE");

        assertThat(response.getDailyPlan()).hasSize(7);
        assertThat(response.getDailyPlan().get(0).getDayNumber()).isEqualTo(1);
        assertThat(response.getDailyPlan().get(6).getDayNumber()).isEqualTo(7);
    }

    @Test
    @DisplayName("1-day, 3-day and 7-day generation succeeds for supported destinations")
    void supportsRequiredTripLengths() {
        assertThat(generator.generate(request("Munich", 1, "MODERATE", List.of(), List.of()),
                "AI_PROVIDER_UNAVAILABLE").getDailyPlan()).hasSize(1);
        assertThat(generator.generate(request("Berlin", 3, "MODERATE", List.of(), List.of()),
                "AI_PROVIDER_UNAVAILABLE").getDailyPlan()).hasSize(3);
        assertThat(generator.generate(request("Kyoto", 7, "MODERATE", List.of(), List.of()),
                "AI_PROVIDER_UNAVAILABLE").getDailyPlan()).hasSize(7);
    }

    @Test
    @DisplayName("1-day trips do not include day trips")
    void oneDayTripsDoNotIncludeDayTrips() {
        ItineraryResponse response = generator.generate(request("Queenstown", 1, "PACKED",
                List.of(), List.of()), "AI_PROVIDER_UNAVAILABLE");

        assertThat(activityTitles(response))
                .noneMatch(title -> title.toLowerCase().contains("day trip"));
    }

    @Test
    @DisplayName("SLOW, MODERATE and PACKED pace affect daily activity count")
    void paceAffectsDailyActivityCount() {
        int slowCount = totalActivities(generator.generate(request("Berlin", 3, "SLOW",
                List.of(), List.of()), "AI_PROVIDER_UNAVAILABLE"));
        int moderateCount = totalActivities(generator.generate(request("Berlin", 3, "MODERATE",
                List.of(), List.of()), "AI_PROVIDER_UNAVAILABLE"));
        int packedCount = totalActivities(generator.generate(request("Berlin", 3, "PACKED",
                List.of(), List.of()), "AI_PROVIDER_UNAVAILABLE"));

        assertThat(slowCount).isLessThan(moderateCount);
        assertThat(moderateCount).isLessThan(packedCount);
    }

    @Test
    @DisplayName("must-see and avoid attractions are applied")
    void appliesMustSeeAndAvoidAttractions() {
        ItineraryResponse response = generator.generate(request("Berlin", 3, "MODERATE",
                List.of("Museum Island"), List.of("Brandenburg Gate")), "AI_PROVIDER_UNAVAILABLE");

        assertThat(activityTitles(response)).contains("Museum Island");
        assertThat(activityTitles(response)).doesNotContain("Brandenburg Gate");
    }

    @Test
    @DisplayName("sets fallback metadata")
    void setsFallbackMetadata() {
        ItineraryResponse response = generator.generate(request("Prauge", 3, "MODERATE",
                List.of(), List.of()), "AI_PROVIDER_UNAVAILABLE");

        assertThat(response.getTokensUsed()).isZero();
        assertThat(response.getFallbackUsed()).isTrue();
        assertThat(response.getFallbackReason()).isEqualTo("AI_PROVIDER_UNAVAILABLE");
        assertThat(response.getTripTitle()).contains("Prague, Czechia");
    }

    private GenerateItineraryRequest request(String destination, int days, String pace,
                                             List<String> mustSee,
                                             List<String> avoid) {
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        return new GenerateItineraryRequest(
                null,
                new TripConstraints(destination, startDate, startDate.plusDays(days - 1L),
                        "MODERATE", new TripConstraints.Travelers(2, 0), null),
                null,
                null,
                new GenerateItineraryRequest.ItineraryPreferences(
                        true, true, false, pace, mustSee, avoid));
    }

    private int totalActivities(ItineraryResponse response) {
        return response.getDailyPlan().stream()
                .mapToInt(day -> day.getActivities().size())
                .sum();
    }

    private List<String> activityTitles(ItineraryResponse response) {
        return response.getDailyPlan().stream()
                .flatMap(day -> day.getActivities().stream())
                .map(ItineraryResponse.Activity::getTitle)
                .toList();
    }
}
