package pse.trippy.aiservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GenerateItineraryRequest(
        UUID tripId,

        @NotNull(message = "Constraints are required")
        @Valid
        TripConstraints constraints,

        @Size(max = 1000, message = "User prompt must be at most 1000 characters")
        String userPrompt,

        String tone,

        ItineraryPreferences preferences
) {
    public record ItineraryPreferences(
            boolean includeTransport,
            boolean includeMeals,
            boolean includeAccommodation,
            String pacePreference,
            java.util.List<String> mustSeeAttractions,
            java.util.List<String> avoidAttractions
    ) {
    }
}
