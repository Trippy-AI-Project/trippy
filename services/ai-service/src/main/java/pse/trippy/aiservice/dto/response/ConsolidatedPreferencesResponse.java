package pse.trippy.aiservice.dto.response;

import java.util.List;

public record ConsolidatedPreferencesResponse(
        String recommendedBudget,
        String recommendedPace,
        List<String> sharedInterests,
        List<String> mustSeeConsensus,
        List<Conflict> conflicts,
        String suggestedPrompt
) {
    public record Conflict(
            String topic,
            String description
    ) {
    }
}
