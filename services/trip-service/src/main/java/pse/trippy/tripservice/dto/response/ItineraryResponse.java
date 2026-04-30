package pse.trippy.tripservice.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ItineraryResponse(
        UUID tripId,
        List<DayPlanResponse> dayPlans,
        Instant createdAt,
        Instant updatedAt
) {
}
