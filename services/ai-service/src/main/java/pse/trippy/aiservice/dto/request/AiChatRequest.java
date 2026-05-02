package pse.trippy.aiservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import pse.trippy.aiservice.dto.response.ItineraryResponse;

import java.util.List;

public record AiChatRequest(
        @NotEmpty
        @Valid
        List<ChatMessage> messages,

        @Size(max = 2000)
        String tripContext,

        List<ItineraryResponse.DayPlan> currentItinerary,

        String destination
) {
    public record ChatMessage(
            @NotBlank
            String role,

            @NotBlank
            @Size(max = 2000)
            String content
    ) {
    }
}
