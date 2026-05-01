package pse.trippy.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentConfirmationRequest(

        @NotBlank(message = "Plan ID is required")
        String planId,

        @NotNull(message = "Payment method ID is required")
        UUID paymentMethodId,

        String promoCode
) {
}
