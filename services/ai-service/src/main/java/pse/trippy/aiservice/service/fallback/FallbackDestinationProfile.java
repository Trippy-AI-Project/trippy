package pse.trippy.aiservice.service.fallback;

import java.math.BigDecimal;
import java.util.List;

public record FallbackDestinationProfile(
        String id,
        String scope,
        String destination,
        String city,
        String country,
        String destinationType,
        List<String> aliases,
        List<String> primaryTags,
        List<String> anchorHighlights,
        String description,
        List<String> highlights,
        BigDecimal estimatedDailyCost,
        String bestTimeToVisit,
        List<Integer> recommendedMonths,
        List<String> tags,
        List<String> budgetLevels,
        int minimumRecommendedDays,
        int idealMaximumDays,
        int suggestedMinimumActivityPoolSize,
        List<FallbackActivity> activities,
        List<FallbackActivity> foodExperiences,
        List<FallbackActivity> eveningOptions,
        List<FallbackActivity> dayTrips,
        List<String> travelTips,
        List<String> packingTips
) {
}
