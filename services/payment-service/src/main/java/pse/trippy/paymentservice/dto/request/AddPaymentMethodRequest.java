package pse.trippy.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddPaymentMethodRequest(

        @NotBlank(message = "Brand is required")
        @Size(max = 20)
        String brand,

        @NotBlank(message = "Last 4 digits are required")
        @Size(min = 4, max = 4, message = "Last4 must be exactly 4 digits")
        String last4,

        @NotNull(message = "Expiry month is required")
        Integer expiryMonth,

        @NotNull(message = "Expiry year is required")
        Integer expiryYear,

        @Size(max = 100)
        String holderName,

        boolean setAsDefault
) {
}
