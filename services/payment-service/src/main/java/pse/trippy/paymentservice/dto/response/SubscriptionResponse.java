package pse.trippy.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SubscriptionResponse(
        UUID subscriptionId,
        String plan,
        String status,
        LocalDate currentPeriodStart,
        LocalDate currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        BigDecimal priceAmount,
        String currency
) {
}
