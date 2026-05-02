package pse.trippy.aiservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record DestinationSuggestionRequest(
        String prompt,
        String city,
        List<String> interests,
        String budget,
        String travelStyle,
        @Positive @Max(30) int duration,
        String region,
        String month,
        Integer people,
        String diet,
        String preferences,
        String customNotes
) {
}
