package pse.trippy.aiservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record GroupPreferenceRequest(
        @NotNull(message = "Trip ID is required")
        UUID tripId,

        @NotEmpty(message = "At least one preference is required")
        List<@Valid UserPreference> preferences
) {
    public record UserPreference(
            UUID userId,
            List<String> interests,
            String budgetPreference,
            String pacePreference,
            List<String> mustSee,
            List<String> avoid
    ) {
    }
}
