package pse.trippy.aiservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class GenerateItineraryRequest {

    private String tripId;

    @NotNull
    private Constraints constraints;

    @Size(max = 1000)
    private String userPrompt;

    private String tone = "BALANCED"; // RELAXED, ADVENTUROUS, FAMILY, ROMANTIC, CULTURAL, BALANCED

    private Preferences preferences;

    @Data
    public static class Constraints {
        @NotBlank
        private String destination;

        @NotNull
        private LocalDate startDate;

        @NotNull
        private LocalDate endDate;

        private String budgetLevel = "MODERATE"; // ECONOMY, MODERATE, LUXURY

        private Integer adults = 1;
        private Integer children = 0;

        private String accommodationType = "ANY";
    }

    @Data
    public static class Preferences {
        private boolean includeTransport = true;
        private boolean includeMeals = true;
        private boolean includeAccommodation = false;
        private String pacePreference = "MODERATE"; // SLOW, MODERATE, PACKED
        private List<String> mustSeeAttractions;
        private List<String> avoidAttractions;
    }
}
