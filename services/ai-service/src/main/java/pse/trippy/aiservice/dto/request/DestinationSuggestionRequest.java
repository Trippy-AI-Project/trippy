package pse.trippy.aiservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DestinationSuggestionRequest {

    @NotBlank
    @Size(max = 500)
    private String prompt;

    private String budget; // LOW, MEDIUM, HIGH

    @Max(30)
    private Integer durationDays;

    private String travelMonth;

    private List<String> interests;

    private List<String> excludeCountries;
}
