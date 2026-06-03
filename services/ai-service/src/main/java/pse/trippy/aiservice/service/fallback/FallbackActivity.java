package pse.trippy.aiservice.service.fallback;

import java.util.List;

public record FallbackActivity(
        String id,
        String title,
        String description,
        String location,
        String category,
        String priority,
        int minimumTripDays,
        int durationMinutes,
        String estimatedCost,
        String costLevel,
        List<String> interests,
        String preferredTime,
        String cluster,
        boolean suitableInRain,
        boolean bookingRecommended
) {
}
