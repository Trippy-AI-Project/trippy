package pse.trippy.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("features")
    private String features;

    @JsonProperty("billingCycle")
    private String billingCycle;
}
