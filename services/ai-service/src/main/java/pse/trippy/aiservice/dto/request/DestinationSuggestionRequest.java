package pse.trippy.aiservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DestinationSuggestionRequest(
        @NotEmpty(message = "At least one interest is required")
        @Size(max = 10, message = "Maximum 10 interests allowed")
        List<String> interests,

        @NotNull(message = "Budget is required")
        String budget,

        String travelStyle,

        @Min(value = 1, message = "Duration must be at least 1 day")
        @Max(value = 90, message = "Duration must be at most 90 days")
        int duration,

        String region,

        String month
) {
}
