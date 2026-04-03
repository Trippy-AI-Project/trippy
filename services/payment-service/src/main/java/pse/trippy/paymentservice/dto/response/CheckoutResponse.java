package pse.trippy.paymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResponse {

    private UUID transactionId;
    private String status;
    private String plan;
    private Amount amount;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Amount {
        private BigDecimal value;
        private String currency;
    }
}
