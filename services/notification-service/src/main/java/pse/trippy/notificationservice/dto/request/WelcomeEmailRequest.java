package pse.trippy.notificationservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record WelcomeEmailRequest(
        @NotBlank @Email String to,
        @NotBlank String userName,
        String dashboardUrl
) {
}
