package pse.trippy.notificationservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Email template rendering")
class EmailTemplateRenderingTest {

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    private String render(String template, Map<String, Object> variables) {
        Context ctx = new Context();
        ctx.setVariables(variables);
        return templateEngine.process("email/" + template, ctx);
    }

    @Test
    @DisplayName("welcome template renders with user name and dashboard URL")
    void welcomeTemplate() {
        String html = render("welcome", Map.of(
                "userName", "Alice",
                "dashboardUrl", "https://trippy.app/dashboard"));

        assertThat(html).contains("Alice");
        assertThat(html).contains("https://trippy.app/dashboard");
        assertThat(html).contains("Welcome to Trippy");
    }

    @Test
    @DisplayName("email-verification template renders with code")
    void verificationTemplate() {
        String html = render("email-verification", Map.of(
                "userName", "Bob",
                "verificationCode", "482916"));

        assertThat(html).contains("Bob");
        assertThat(html).contains("482916");
        assertThat(html).contains("15 minutes");
    }

    @Test
    @DisplayName("password-reset template renders with reset link")
    void passwordResetTemplate() {
        String html = render("password-reset", Map.of(
                "userName", "Carol",
                "resetLink", "https://trippy.app/reset?token=abc123"));

        assertThat(html).contains("Carol");
        assertThat(html).contains("https://trippy.app/reset?token=abc123");
        assertThat(html).contains("1 hour");
    }

    @Test
    @DisplayName("trip-invite template renders invite details")
    void tripInviteTemplate() {
        String html = render("trip-invite", Map.of(
                "inviteeName", "Bob",
                "inviterName", "Jane",
                "tripTitle", "Summer in Barcelona",
                "link", "https://trippy.app/dashboard/trips/1"));

        assertThat(html).contains("Bob");
        assertThat(html).contains("Jane");
        assertThat(html).contains("Summer in Barcelona");
        assertThat(html).contains("https://trippy.app/dashboard/trips/1");
    }

    @Test
    @DisplayName("trip-joined template renders participant details")
    void tripJoinedTemplate() {
        String html = render("trip-joined", Map.of(
                "userName", "Jane",
                "joinerName", "Bob",
                "tripTitle", "Summer in Barcelona",
                "link", "https://trippy.app/dashboard/trips/1"));

        assertThat(html).contains("Jane");
        assertThat(html).contains("Bob");
        assertThat(html).contains("Summer in Barcelona");
    }

    @Test
    @DisplayName("itinerary-ready template renders trip details")
    void itineraryReadyTemplate() {
        String html = render("itinerary-ready", Map.of(
                "userName", "Alice",
                "tripTitle", "Kyoto",
                "link", "https://trippy.app/dashboard/trips/2"));

        assertThat(html).contains("Alice");
        assertThat(html).contains("Kyoto");
        assertThat(html).contains("View Itinerary");
    }

    @Test
    @DisplayName("renderTemplate rejects unsafe template names")
    void renderTemplateRejectsUnsafeTemplateNames() {
        assertThatThrownBy(() -> emailService.renderTemplate("../welcome", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
