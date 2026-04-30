package pse.trippy.aiservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationSuggestionResponse {

    private List<DestinationSuggestion> suggestions;
    private Instant generatedAt;
    private boolean cached;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DestinationSuggestion {
        private String city;
        private String country;
        private String estimatedDailyCost;
        private String bestTimeToVisit;
        private List<String> highlights;
        private String reason;
        private Double matchScore;
    }
}
