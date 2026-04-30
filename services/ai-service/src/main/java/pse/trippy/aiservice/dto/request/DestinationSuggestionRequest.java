package pse.trippy.aiservice.dto.request;

import java.util.List;

public record DestinationSuggestionRequest(
        List<String> interests,
        String budget,
        String travelStyle,
        int duration,
        String region,
        String month
) {
}
