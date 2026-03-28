package pse.trippy.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
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

    @JsonProperty("subscription")
    private SubscriptionInfoDto subscription;

    @JsonProperty("verificationReference")
    private String verificationReference;

    @JsonProperty("confirmedAt")
    private Instant confirmedAt;

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

/**
 * Contract-aligned subscription info DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class SubscriptionInfoDto {
    @JsonProperty("subscriptionId")
    private UUID subscriptionId;

    @JsonProperty("plan")
    private String plan;

    @JsonProperty("status")
    private String status;

    @JsonProperty("currentPeriodStart")
    private String currentPeriodStart;

    @JsonProperty("currentPeriodEnd")
    private String currentPeriodEnd;

    @JsonProperty("cancelAtPeriodEnd")
    private boolean cancelAtPeriodEnd;

    @JsonProperty("price")
    private MoneyDto price;
}

/**
 * Contract-aligned money DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class MoneyDto {
    @JsonProperty("amount")
    private double amount;

    @JsonProperty("currency")
    private String currency;
}

/**
 * Contract-aligned subscription info DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class SubscriptionInfoDto {
    @JsonProperty("subscriptionId")
    private UUID subscriptionId;

    @JsonProperty("plan")
    private String plan;

    @JsonProperty("status")
    private String status;

    @JsonProperty("currentPeriodStart")
    private String currentPeriodStart;

    @JsonProperty("currentPeriodEnd")
    private String currentPeriodEnd;

    @JsonProperty("cancelAtPeriodEnd")
    private boolean cancelAtPeriodEnd;

    @JsonProperty("price")
    private MoneyDto price;
}

/**
 * Contract-aligned money DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class MoneyDto {
    @JsonProperty("amount")
    private double amount;

    @JsonProperty("currency")
    private String currency;
}

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
