package pse.trippy.aiservice.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ItineraryGenerationResponse(
        UUID generationId,
        UUID tripId,
        List<DayPlanResponse> days,
        String overview,
        String estimatedTotalCost,
        Instant generatedAt,
        int tokensUsed,
        boolean cached
) {
}
