package pse.trippy.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for checkout response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponseDto {

    @JsonProperty("transactionId")
    private UUID transactionId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("plan")
    private String plan;

    @JsonProperty("amount")
    private AmountDto amount;

    @JsonProperty("message")
    private String message;

    /**
     * Nested DTO for amount information.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AmountDto {
        @JsonProperty("value")
        private BigDecimal value;

        @JsonProperty("currency")
        private String currency;
    }
}
