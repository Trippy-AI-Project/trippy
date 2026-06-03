package pse.trippy.aiservice.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DestinationSuggestion(
        String destination,
        String country,
        String description,
        List<String> highlights,
        BigDecimal estimatedDailyCost,
        String bestTimeToVisit,
        String googleMapsUrl,
        double matchScore
) {
}
