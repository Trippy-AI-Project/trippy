package pse.trippy.aiservice.dto.response;

import java.util.List;

public record AiChatResponse(
        String reply,
        List<ItineraryResponse.DayPlan> updatedItinerary,
        List<Change> changes,
        boolean hasModification
) {
    public record Change(
            String type,
            Integer dayNumber,
            String removed,
            String added,
            String summary
    ) {
    }
}
