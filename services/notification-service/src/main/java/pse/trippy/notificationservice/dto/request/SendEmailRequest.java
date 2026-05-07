package pse.trippy.notificationservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record SendEmailRequest(
        @NotBlank @Email String to,
        @NotBlank @Size(max = 500) String subject,
        @NotBlank @Pattern(regexp = "[a-z0-9-]+") String templateName,
        @NotNull Map<String, Object> templateVariables
) {
}
