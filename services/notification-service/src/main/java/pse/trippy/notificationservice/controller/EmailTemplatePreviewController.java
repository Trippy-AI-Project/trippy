package pse.trippy.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.notificationservice.service.EmailService;

import java.util.Map;

@RestController
@RequestMapping("/notifications/templates")
@Profile("dev")
@RequiredArgsConstructor
public class EmailTemplatePreviewController {

    private final EmailService emailService;

    @GetMapping(value = "/{name}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@PathVariable String name) {
        return ResponseEntity.ok(emailService.renderTemplate(name, sampleVariables(name)));
    }

    private Map<String, Object> sampleVariables(String name) {
        return switch (name) {
            case "trip-invite", "trip-invitation" -> Map.of(
                    "inviteeName", "Sam",
                    "inviterName", "Mira",
                    "tripTitle", "Berlin Weekend",
                    "dashboardUrl", "https://trippy.app/dashboard");
            case "trip-joined", "invitation-accepted" -> Map.of(
                    "inviterName", "Mira",
                    "inviteeName", "Sam",
                    "tripTitle", "Berlin Weekend",
                    "dashboardUrl", "https://trippy.app/dashboard");
            case "payment-success" -> Map.of(
                    "userName", "Sam",
                    "amount", "19.99",
                    "planName", "Premium",
                    "dashboardUrl", "https://trippy.app/dashboard");
            case "payment-failed" -> Map.of(
                    "userName", "Sam",
                    "dashboardUrl", "https://trippy.app/dashboard");
            case "itinerary-ready" -> Map.of(
                    "userName", "Sam",
                    "tripTitle", "Kyoto Spring",
                    "destination", "Kyoto",
                    "tripUrl", "https://trippy.app/dashboard/trips/demo");
            case "password-reset" -> Map.of(
                    "userName", "Sam",
                    "resetLink", "https://trippy.app/reset?token=demo");
            default -> Map.of(
                    "userName", "Sam",
                    "title", "Trippy update",
                    "message", "This is a preview notification.",
                    "actionUrl", "https://trippy.app/dashboard",
                    "dashboardUrl", "https://trippy.app/dashboard");
        };
    }
}
