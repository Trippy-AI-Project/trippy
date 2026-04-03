package pse.trippy.notificationservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerificationEmailRequest(
        @NotBlank @Email String to,
        @NotBlank String userName,
        @NotBlank String verificationCode
) {
}
