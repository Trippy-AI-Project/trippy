package pse.trippy.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String type,
        String status,
        String description,
        Instant createdAt
) {
}
