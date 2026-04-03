package pse.trippy.aiservice.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AiUsageResponse(
        UUID userId,
        long totalRequests,
        Map<String, Long> requestsByType,
        Instant lastUsedAt,
        long tokensConsumed
) {
}
