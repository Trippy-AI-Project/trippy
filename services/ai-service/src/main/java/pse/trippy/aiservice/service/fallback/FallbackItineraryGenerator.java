package pse.trippy.aiservice.service.fallback;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pse.trippy.aiservice.dto.request.GenerateItineraryRequest;
import pse.trippy.aiservice.dto.request.TripConstraints;
import pse.trippy.aiservice.dto.response.ItineraryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FallbackItineraryGenerator {

    private static final List<String> ACTIVITY_TIMES = List.of("09:00", "10:45", "12:45", "15:00", "17:30", "19:30");
    private static final String DEFAULT_REASON = "AI_PROVIDER_UNAVAILABLE";

    private final FallbackDestinationCatalogue catalogue;

    public Optional<FallbackDestinationProfile> findProfile(String destination) {
        return catalogue.findBestMatch(destination);
    }

    public ItineraryResponse generate(GenerateItineraryRequest request, String fallbackReason) {
        String reason = fallbackReason == null || fallbackReason.isBlank() ? DEFAULT_REASON : fallbackReason;
        return catalogue.findBestMatch(request.constraints().destination())
                .map(profile -> generateForProfile(request, profile, reason))
                .orElseGet(() -> generateUnsupportedDestinationFallback(request, reason));
    }

    private ItineraryResponse generateForProfile(GenerateItineraryRequest request,
                                                 FallbackDestinationProfile profile,
                                                 String fallbackReason) {
        TripConstraints constraints = request.constraints();
        int days = requestedDays(constraints);
        Preferences preferences = Preferences.from(request.preferences());
        List<FallbackActivity> corePool = prioritizedActivities(profile, preferences, days);
        Set<String> usedTitles = new HashSet<>();
        List<ItineraryResponse.DayPlan> dayPlans = new ArrayList<>();

        for (int dayNumber = 1; dayNumber <= days; dayNumber++) {
            LocalDate date = constraints.startDate().plusDays(dayNumber - 1L);
            List<ItineraryResponse.Activity> activities = new ArrayList<>();

            boolean dayTripDay = dayNumber == 5 && days >= 5 && !safeList(profile.dayTrips()).isEmpty();
            if (dayTripDay) {
                addActivity(activities, firstAvailable(profile.dayTrips(), preferences, usedTitles)
                        .orElse(profile.dayTrips().get(0)), usedTitles);
            } else {
                int targetCoreCount = targetCoreActivityCount(preferences.pacePreference(), dayNumber, days);
                addMustSeeRequests(activities, corePool, preferences, usedTitles, dayNumber);
                while (coreActivityCount(activities) < targetCoreCount) {
                    Optional<FallbackActivity> next = nextActivity(corePool, usedTitles, dayNumber);
                    if (next.isPresent()) {
                        addActivity(activities, next.get(), usedTitles);
                    } else {
                        activities.add(flexibleActivity(profile, dayNumber, activities.size()));
                        break;
                    }
                }
            }

            if (preferences.includeMeals()) {
                Optional<FallbackActivity> food = firstAvailable(profile.foodExperiences(), preferences, usedTitles);
                food.ifPresent(activity -> addActivity(activities, activity, usedTitles));
            }

            if (shouldAddEvening(dayNumber, days, preferences.pacePreference())) {
                Optional<FallbackActivity> evening = firstAvailable(profile.eveningOptions(), preferences, usedTitles);
                evening.ifPresent(activity -> addActivity(activities, activity, usedTitles));
            }

            assignTimes(activities);
            dayPlans.add(ItineraryResponse.DayPlan.builder()
                    .dayNumber(dayNumber)
                    .date(date.toString())
                    .title(dayTitle(profile, dayNumber, days, dayTripDay))
                    .weather(unavailableWeather())
                    .activities(activities)
                    .build());
        }

        return ItineraryResponse.builder()
                .generationId(UUID.randomUUID())
                .tripTitle(days + " Days in " + profile.destination())
                .summary("Fallback itinerary generated from Trippy's static destination catalogue for "
                        + profile.destination() + ".")
                .totalEstimatedCost(totalCostEstimate(profile, days))
                .dailyPlan(dayPlans)
                .packingTips(safeList(profile.packingTips()))
                .travelTips(safeList(profile.travelTips()))
                .generatedAt(Instant.now())
                .tokensUsed(0)
                .fallbackUsed(true)
                .fallbackReason(fallbackReason)
                .build();
    }

    private ItineraryResponse generateUnsupportedDestinationFallback(GenerateItineraryRequest request,
                                                                    String fallbackReason) {
        TripConstraints constraints = request.constraints();
        int days = requestedDays(constraints);
        List<ItineraryResponse.DayPlan> dayPlans = new ArrayList<>();
        for (int dayNumber = 1; dayNumber <= days; dayNumber++) {
            LocalDate date = constraints.startDate().plusDays(dayNumber - 1L);
            ItineraryResponse.Activity orientation = ItineraryResponse.Activity.builder()
                    .time("09:30")
                    .durationMinutes(120)
                    .title("Flexible orientation in " + constraints.destination())
                    .description("The fallback catalogue does not have a full profile for this destination. "
                            + "Use this block to start with a central, well-reviewed area.")
                    .location(constraints.destination())
                    .category("FREE_TIME")
                    .estimatedCost("Varies")
                    .tips("Regenerate when AI is reachable for a destination-specific plan.")
                    .bookingRequired(false)
                    .build();
            dayPlans.add(ItineraryResponse.DayPlan.builder()
                    .dayNumber(dayNumber)
                    .date(date.toString())
                    .title("Day " + dayNumber + " in " + constraints.destination())
                    .weather(unavailableWeather())
                    .activities(List.of(orientation))
                    .build());
        }

        return ItineraryResponse.builder()
                .generationId(UUID.randomUUID())
                .tripTitle(days + " Days in " + constraints.destination())
                .summary("Fallback generated because AI was unavailable, but this destination is not in the "
                        + "static fallback catalogue.")
                .totalEstimatedCost("Estimate unavailable")
                .dailyPlan(dayPlans)
                .packingTips(List.of("Comfortable walking shoes", "Weather-appropriate layers"))
                .travelTips(List.of("Confirm current opening days and tickets before visiting major attractions.",
                        "Use this as a temporary plan and regenerate when AI is reachable."))
                .generatedAt(Instant.now())
                .tokensUsed(0)
                .fallbackUsed(true)
                .fallbackReason(fallbackReason)
                .build();
    }

    private List<FallbackActivity> prioritizedActivities(FallbackDestinationProfile profile,
                                                         Preferences preferences,
                                                         int days) {
        List<FallbackActivity> mustSee = safeList(profile.activities()).stream()
                .filter(activity -> "MUST_SEE".equals(activity.priority()))
                .filter(activity -> days > 1 || !isDayTripActivity(activity))
                .filter(activity -> !preferences.isAvoided(activity.title()))
                .sorted(Comparator.comparingInt(FallbackActivity::minimumTripDays))
                .toList();
        List<FallbackActivity> requested = safeList(profile.activities()).stream()
                .filter(activity -> preferences.isRequested(activity.title()))
                .filter(activity -> days > 1 || !isDayTripActivity(activity))
                .filter(activity -> !preferences.isAvoided(activity.title()))
                .toList();
        List<FallbackActivity> recommended = safeList(profile.activities()).stream()
                .filter(activity -> !"MUST_SEE".equals(activity.priority()))
                .filter(activity -> days > 1 || !isDayTripActivity(activity))
                .filter(activity -> activity.minimumTripDays() <= Math.max(days, 4))
                .filter(activity -> !preferences.isAvoided(activity.title()))
                .sorted(Comparator
                        .comparingInt(FallbackActivity::minimumTripDays)
                        .thenComparing(FallbackActivity::title))
                .toList();

        LinkedHashSet<FallbackActivity> ordered = new LinkedHashSet<>();
        ordered.addAll(requested);
        ordered.addAll(mustSee);
        ordered.addAll(recommended);
        return List.copyOf(ordered);
    }

    private boolean isDayTripActivity(FallbackActivity activity) {
        return activity.title().toLowerCase(Locale.ROOT).contains("day trip")
                || "FULL_DAY".equals(activity.preferredTime())
                || activity.durationMinutes() >= 360;
    }

    private void addMustSeeRequests(List<ItineraryResponse.Activity> activities,
                                    List<FallbackActivity> corePool,
                                    Preferences preferences,
                                    Set<String> usedTitles,
                                    int dayNumber) {
        if (dayNumber > 2) {
            return;
        }
        for (FallbackActivity activity : corePool) {
            if (preferences.isRequested(activity.title()) && !usedTitles.contains(normalizeTitle(activity.title()))) {
                addActivity(activities, activity, usedTitles);
            }
        }
    }

    private Optional<FallbackActivity> nextActivity(List<FallbackActivity> pool,
                                                   Set<String> usedTitles,
                                                   int dayNumber) {
        return pool.stream()
                .filter(activity -> !usedTitles.contains(normalizeTitle(activity.title())))
                .filter(activity -> activity.minimumTripDays() <= Math.max(dayNumber, 1))
                .findFirst()
                .or(() -> pool.stream()
                        .filter(activity -> !usedTitles.contains(normalizeTitle(activity.title())))
                        .findFirst());
    }

    private Optional<FallbackActivity> firstAvailable(List<FallbackActivity> pool,
                                                      Preferences preferences,
                                                      Set<String> usedTitles) {
        return safeList(pool).stream()
                .filter(activity -> !preferences.isAvoided(activity.title()))
                .filter(activity -> !usedTitles.contains(normalizeTitle(activity.title())))
                .findFirst();
    }

    private void addActivity(List<ItineraryResponse.Activity> activities,
                             FallbackActivity activity,
                             Set<String> usedTitles) {
        usedTitles.add(normalizeTitle(activity.title()));
        activities.add(toResponseActivity(activity));
    }

    private ItineraryResponse.Activity toResponseActivity(FallbackActivity fallbackActivity) {
        return ItineraryResponse.Activity.builder()
                .durationMinutes(fallbackActivity.durationMinutes())
                .title(fallbackActivity.title())
                .description(fallbackActivity.description())
                .location(fallbackActivity.location())
                .category(fallbackActivity.category())
                .estimatedCost(fallbackActivity.estimatedCost())
                .tips(tipsFor(fallbackActivity))
                .bookingRequired(fallbackActivity.bookingRecommended())
                .build();
    }

    private ItineraryResponse.Activity flexibleActivity(FallbackDestinationProfile profile,
                                                        int dayNumber,
                                                        int activityIndex) {
        return ItineraryResponse.Activity.builder()
                .durationMinutes(120)
                .title("Slow exploration block in " + profile.city() + " - Day " + dayNumber)
                .description("Use this lower-pressure block for a neighbourhood walk, cafe pause, rest or "
                        + "weather-safe indoor alternative.")
                .location(profile.city())
                .category("FREE_TIME")
                .estimatedCost("Flexible")
                .tips("Keep this block adaptable instead of forcing another landmark.")
                .bookingRequired(false)
                .time(ACTIVITY_TIMES.get(Math.min(activityIndex, ACTIVITY_TIMES.size() - 1)))
                .build();
    }

    private void assignTimes(List<ItineraryResponse.Activity> activities) {
        for (int index = 0; index < activities.size(); index++) {
            ItineraryResponse.Activity activity = activities.get(index);
            if (activity.getTime() == null || activity.getTime().isBlank()) {
                activity.setTime(ACTIVITY_TIMES.get(Math.min(index, ACTIVITY_TIMES.size() - 1)));
            }
        }
    }

    private int requestedDays(TripConstraints constraints) {
        long days = ChronoUnit.DAYS.between(constraints.startDate(), constraints.endDate()) + 1;
        if (days < 1) {
            throw new IllegalArgumentException("Trip end date must not be before start date");
        }
        return Math.toIntExact(days);
    }

    private int targetCoreActivityCount(String pacePreference, int dayNumber, int totalDays) {
        String pace = pacePreference == null ? "MODERATE" : pacePreference.trim().toUpperCase(Locale.ROOT);
        if ("SLOW".equals(pace)) {
            return totalDays >= 8 ? 1 : 2;
        }
        if ("PACKED".equals(pace)) {
            return dayNumber == 1 ? 4 : 5;
        }
        return totalDays >= 8 ? 2 : 3;
    }

    private int coreActivityCount(List<ItineraryResponse.Activity> activities) {
        return (int) activities.stream()
                .filter(activity -> !"FOOD".equals(activity.getCategory()))
                .filter(activity -> !"NIGHTLIFE".equals(activity.getCategory()))
                .count();
    }

    private boolean shouldAddEvening(int dayNumber, int days, String pacePreference) {
        if (days < 3) {
            return false;
        }
        String pace = pacePreference == null ? "MODERATE" : pacePreference.trim().toUpperCase(Locale.ROOT);
        if ("PACKED".equals(pace)) {
            return dayNumber % 2 == 1;
        }
        if ("SLOW".equals(pace)) {
            return dayNumber == 3 || dayNumber == days;
        }
        return dayNumber == 3 || dayNumber == days || dayNumber % 4 == 0;
    }

    private String dayTitle(FallbackDestinationProfile profile, int dayNumber, int days, boolean dayTripDay) {
        if (dayTripDay) {
            return "Day " + dayNumber + ": Regional day trip from " + profile.city();
        }
        if (dayNumber == 1) {
            return "Day 1: " + profile.city() + " essentials";
        }
        if (days >= 8 && dayNumber > 7) {
            return "Day " + dayNumber + ": Slower local exploration";
        }
        return "Day " + dayNumber + ": " + profile.city() + " at a comfortable pace";
    }

    private String totalCostEstimate(FallbackDestinationProfile profile, int days) {
        BigDecimal dailyCost = profile.estimatedDailyCost() == null ? BigDecimal.ZERO : profile.estimatedDailyCost();
        BigDecimal total = dailyCost.multiply(BigDecimal.valueOf(days));
        return "Approx. €" + total.intValue() + " daily local cost estimate, excluding flights and accommodation";
    }

    private String tipsFor(FallbackActivity activity) {
        if (activity.bookingRecommended()) {
            return "Booking may be useful; confirm current availability before departure.";
        }
        if (activity.suitableInRain()) {
            return "Works as a weather-safe fallback if outdoor plans change.";
        }
        return "Keep timing flexible and check current local conditions.";
    }

    private ItineraryResponse.WeatherSummary unavailableWeather() {
        return ItineraryResponse.WeatherSummary.builder()
                .condition("Forecast unavailable")
                .temperatureCelsius(null)
                .advice("Check the local forecast closer to departure.")
                .build();
    }

    private String normalizeTitle(String value) {
        return FallbackDestinationCatalogue.normalize(value);
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record Preferences(
            boolean includeMeals,
            String pacePreference,
            List<String> mustSeeAttractions,
            List<String> avoidAttractions
    ) {
        static Preferences from(GenerateItineraryRequest.ItineraryPreferences preferences) {
            if (preferences == null) {
                return new Preferences(true, "MODERATE", List.of(), List.of());
            }
            return new Preferences(
                    preferences.includeMeals(),
                    preferences.pacePreference() == null ? "MODERATE" : preferences.pacePreference(),
                    safeList(preferences.mustSeeAttractions()),
                    safeList(preferences.avoidAttractions()));
        }

        boolean isRequested(String title) {
            String normalizedTitle = FallbackDestinationCatalogue.normalize(title);
            return mustSeeAttractions.stream()
                    .map(FallbackDestinationCatalogue::normalize)
                    .anyMatch(value -> !value.isBlank()
                            && (normalizedTitle.contains(value) || value.contains(normalizedTitle)));
        }

        boolean isAvoided(String title) {
            String normalizedTitle = FallbackDestinationCatalogue.normalize(title);
            return avoidAttractions.stream()
                    .map(FallbackDestinationCatalogue::normalize)
                    .anyMatch(value -> !value.isBlank()
                            && (normalizedTitle.contains(value) || value.contains(normalizedTitle)));
        }
    }
}
