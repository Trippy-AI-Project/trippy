package pse.trippy.aiservice.dto.response;

import java.time.Instant;
import java.util.List;

public record DestinationSuggestionResponse(
        List<DestinationSuggestion> suggestions,
        Instant generatedAt
) {
}
