package pse.trippy.notificationservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pse.trippy.notificationservice.dto.request.PasswordResetEmailRequest;
import pse.trippy.notificationservice.dto.request.SendEmailRequest;
import pse.trippy.notificationservice.dto.request.VerificationEmailRequest;
import pse.trippy.notificationservice.dto.request.WelcomeEmailRequest;
import pse.trippy.notificationservice.dto.response.EmailSentResponse;
import pse.trippy.notificationservice.service.EmailService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    @Mock
    private EmailService emailService;

    private EmailController controller;

    @BeforeEach
    void setUp() {
        controller = new EmailController(emailService);
    }

    @Test
    void sendEmail_queuesRequestedTemplate() {
        ResponseEntity<EmailSentResponse> response = controller.sendEmail(new SendEmailRequest(
                "alice@example.com",
                "Custom subject",
                "welcome",
                Map.of("userName", "Alice")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        verify(emailService).sendTemplateEmail(
                "alice@example.com",
                "Custom subject",
                "welcome",
                Map.of("userName", "Alice"));
    }

    @Test
    void sendVerification_queuesVerificationTemplate() {
        controller.sendVerification(new VerificationEmailRequest(
                "alice@example.com",
                "Alice",
                "123456"));

        verify(emailService).sendTemplateEmail(
                eq("alice@example.com"),
                eq("Verify your Trippy account"),
                eq("email-verification"),
                eq(Map.of("userName", "Alice", "verificationCode", "123456")));
    }

    @Test
    void sendWelcome_usesDefaultDashboardUrlWhenMissing() {
        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);

        controller.sendWelcome(new WelcomeEmailRequest("alice@example.com", "Alice", null));

        verify(emailService).sendTemplateEmail(
                eq("alice@example.com"),
                eq("Welcome to Trippy!"),
                eq("welcome"),
                variablesCaptor.capture());
        assertThat(variablesCaptor.getValue()).containsEntry("dashboardUrl", "https://trippy.app/dashboard");
    }

    @Test
    void sendPasswordReset_queuesPasswordResetTemplate() {
        controller.sendPasswordReset(new PasswordResetEmailRequest(
                "alice@example.com",
                "Alice",
                "https://trippy.app/reset?token=abc"));

        verify(emailService).sendTemplateEmail(
                eq("alice@example.com"),
                eq("Reset your Trippy password"),
                eq("password-reset"),
                eq(Map.of("userName", "Alice", "resetLink", "https://trippy.app/reset?token=abc")));
    }
}
