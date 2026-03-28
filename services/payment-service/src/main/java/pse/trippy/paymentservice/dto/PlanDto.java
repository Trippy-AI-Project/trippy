package pse.trippy.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for plan information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDto {

    @JsonProperty("planId")
    private String planId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("price")
    private MoneyDto price;

    @JsonProperty("interval")
    private String interval;

    @JsonProperty("features")
    private List<String> features;

    @JsonProperty("tripsLimit")
    private Integer tripsLimit;

    @JsonProperty("aiGenerationsLimit")
    private Integer aiGenerationsLimit;
}
}
