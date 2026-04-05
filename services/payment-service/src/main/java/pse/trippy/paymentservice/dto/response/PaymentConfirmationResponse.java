package pse.trippy.paymentservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PaymentConfirmationResponse(
        UUID transactionId,
        String status,
        SubscriptionResponse subscription,
        String verificationReference,
        Instant confirmedAt
) {
}
