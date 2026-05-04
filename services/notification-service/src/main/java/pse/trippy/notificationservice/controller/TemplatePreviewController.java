package pse.trippy.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.notificationservice.service.EmailService;

import java.util.Map;
import java.util.Set;

@RestController
@Profile("dev")
@RequestMapping("/notifications/templates")
@RequiredArgsConstructor
public class TemplatePreviewController {

    private static final Set<String> PREVIEWABLE_TEMPLATES = Set.of(
            "email-verification",
            "itinerary-ready",
            "password-reset",
            "payment-failed",
            "payment-success",
            "trip-invite",
            "trip-joined",
            "trip-updated",
            "welcome"
    );

    private final EmailService emailService;

    @GetMapping(value = "/{name}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> preview(@PathVariable String name) {
        if (!PREVIEWABLE_TEMPLATES.contains(name)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(emailService.renderTemplate(name, sampleVariables()));
    }

    private Map<String, Object> sampleVariables() {
        return Map.ofEntries(
                Map.entry("userName", "Alex"),
                Map.entry("inviteeName", "Alex"),
                Map.entry("inviterName", "Priya"),
                Map.entry("joinerName", "Sam"),
                Map.entry("participantName", "Sam"),
                Map.entry("tripTitle", "Lisbon Weekend"),
                Map.entry("tripName", "Lisbon Weekend"),
                Map.entry("amount", "29.00"),
                Map.entry("planName", "Premium"),
                Map.entry("generationId", "00000000-0000-0000-0000-000000000001"),
                Map.entry("verificationCode", "482916"),
                Map.entry("resetLink", "https://trippy.app/reset?token=preview"),
                Map.entry("dashboardUrl", "https://trippy.app/dashboard"),
                Map.entry("link", "https://trippy.app/dashboard/trips/preview")
        );
    }
}
