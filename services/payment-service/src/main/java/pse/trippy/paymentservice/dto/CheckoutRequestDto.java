package pse.trippy.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for checkout request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequestDto {

    @JsonProperty("planId")
    private String planId;

    @JsonProperty("paymentMethodId")
    private String paymentMethodId;
}
