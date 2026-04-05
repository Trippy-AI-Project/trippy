package pse.trippy.paymentservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PaymentMethodResponse(
        UUID paymentMethodId,
        String brand,
        String last4,
        Integer expiryMonth,
        Integer expiryYear,
        boolean isDefault,
        Instant createdAt
) {
}
