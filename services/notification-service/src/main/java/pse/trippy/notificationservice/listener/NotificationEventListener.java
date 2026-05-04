package pse.trippy.notificationservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pse.trippy.notificationservice.model.enums.NotificationType;
import pse.trippy.notificationservice.service.EmailService;
import pse.trippy.notificationservice.service.NotificationService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private static final String DASHBOARD_URL = "https://trippy.app/dashboard";

    private final EmailService emailService;
    private final NotificationService notificationService;

    @RabbitListener(queues = "notification.events",
            messageConverter = "jsonMessageConverter")
    public void handleEvent(Object payload,
                            @Header("amqp_receivedRoutingKey") String routingKey) {
        log.info("Received event with routing key: {}", routingKey);

        switch (routingKey) {
            case "user.registered" -> handleUserRegistered(payload);
            case "user.email.verified" -> handleUserEmailVerified(payload);
            case "user.password.reset" -> handlePasswordReset(payload);
            case "trip.invitation.created", "trip.participant.invited" -> handleTripInvitation(payload);
            case "trip.invitation.accepted", "trip.participant.joined" -> handleTripJoined(payload);
            case "trip.updated" -> handleTripUpdated(payload);
            case "payment.completed" -> handlePaymentCompleted(payload);
            case "payment.failed" -> handlePaymentFailed(payload);
            case "ai.itinerary.generated" -> handleItineraryGenerated(payload);
            default -> log.warn("Unknown routing key: {}", routingKey);
        }
    }

    void handleUserRegistered(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = text(map, "email");
            String displayName = text(map, "displayName", "userName", "name");
            String userId = text(map, "userId");
            String verificationToken = text(map, "verificationToken", "verificationCode", "token");

            log.info("Processing user.registered for {}", email);
            sendTemplate(email, "Verify your Trippy account", "email-verification",
                    variables("userName", fallback(displayName, "Traveler"),
                            "verificationCode", verificationToken));

            createNotification(userId, NotificationType.EMAIL_VERIFICATION,
                    "Verify your email",
                    "Use the verification code we emailed to activate your Trippy account.",
                    "/verify-email",
                    metadata(map, "userId"));
        }
    }

    void handleUserEmailVerified(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = text(map, "email");
            String displayName = text(map, "displayName", "userName", "name");
            String userId = text(map, "userId");
            String resolvedDisplayName = fallback(displayName, "Traveler");

            log.info("Processing user.email.verified for {}", email);
            sendTemplate(email, "Welcome to Trippy!", "welcome",
                    variables("userName", resolvedDisplayName,
                            "dashboardUrl", DASHBOARD_URL));

            createNotification(userId, NotificationType.WELCOME,
                    "Welcome to Trippy!",
                    "Your email is verified and your account is ready.",
                    "/dashboard",
                    metadata(map, "userId"));
        }
    }

    void handlePasswordReset(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = text(map, "email");
            String userName = fallback(text(map, "userName", "displayName", "name"), "Traveler");
            String userId = text(map, "userId");
            String resetLink = fallback(text(map, "resetLink", "link"), DASHBOARD_URL);

            log.info("Processing user.password.reset for {}", email);
            sendTemplate(email, "Reset your Trippy password", "password-reset",
                    variables("userName", userName,
                            "resetLink", resetLink));

            createNotification(userId, NotificationType.SYSTEM,
                    "Password reset requested",
                    "We sent password reset instructions to your email.",
                    "/login",
                    metadata(map, "userId"));
        }
    }

    void handleTripInvitation(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String inviteeEmail = text(map, "inviteeEmail", "email");
            String inviteeName = fallback(text(map, "inviteeName", "userName", "displayName"), "Traveler");
            String inviterName = fallback(text(map, "inviterName", "actorName"), "Someone");
            String tripTitle = fallback(text(map, "tripTitle", "tripName", "title"), "a trip");
            String inviteeId = text(map, "inviteeId", "userId");
            String tripId = text(map, "tripId");
            String actionUrl = fallback(text(map, "actionUrl", "inviteLink", "link"), tripUrl(tripId));

            log.info("Processing trip invitation for {}", inviteeEmail);
            sendTemplate(inviteeEmail, inviterName + " invited you to " + tripTitle,
                    "trip-invite",
                    variables("inviteeName", inviteeName,
                            "userName", inviteeName,
                            "inviterName", inviterName,
                            "tripTitle", tripTitle,
                            "tripName", tripTitle,
                            "dashboardUrl", actionUrl,
                            "link", actionUrl));

            createNotification(inviteeId, NotificationType.TRIP_INVITE,
                    "Trip Invitation",
                    inviterName + " invited you to " + tripTitle,
                    actionUrl,
                    metadata(map, "tripId", "inviterId", "inviteeId"));
        }
    }

    void handleInvitationAccepted(Object payload) {
        handleTripJoined(payload);
    }

    void handleTripJoined(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = text(map, "inviterEmail", "email", "ownerEmail");
            String userName = fallback(text(map, "inviterName", "userName", "displayName"), "Traveler");
            String joinerName = fallback(text(map, "inviteeName", "participantName", "joinedBy"), "A traveler");
            String tripTitle = fallback(text(map, "tripTitle", "tripName", "title"), "your trip");
            String userId = text(map, "inviterId", "ownerId", "userId");
            String tripId = text(map, "tripId");
            String actionUrl = fallback(text(map, "actionUrl", "link"), tripUrl(tripId));

            log.info("Processing trip joined notification for {}", email);
            sendTemplate(email, joinerName + " joined " + tripTitle,
                    "trip-joined",
                    variables("userName", userName,
                            "joinerName", joinerName,
                            "participantName", joinerName,
                            "tripTitle", tripTitle,
                            "tripName", tripTitle,
                            "dashboardUrl", actionUrl,
                            "link", actionUrl));

            createNotification(userId, NotificationType.TRIP_JOINED,
                    "Trip Joined",
                    joinerName + " joined " + tripTitle,
                    actionUrl,
                    metadata(map, "tripId", "inviteeId", "participantId"));
        }
    }

    void handleTripUpdated(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = text(map, "email");
            String userName = fallback(text(map, "userName", "displayName"), "Traveler");
            String userId = text(map, "userId");
            String tripTitle = fallback(text(map, "tripTitle", "tripName", "title"), "your trip");
            String updatedBy = fallback(text(map, "updatedBy", "actorName"), "");
            String tripId = text(map, "tripId");
            String actionUrl = fallback(text(map, "actionUrl", "link"), tripUrl(tripId));

            log.info("Processing trip.updated for {}", email);
            sendTemplate(email, "Trip updated: " + tripTitle,
                    "trip-updated",
                    variables("userName", userName,
                            "tripTitle", tripTitle,
                            "updatedBy", updatedBy,
                            "dashboardUrl", actionUrl));

            createNotification(userId, NotificationType.TRIP_UPDATED,
                    "Trip Updated",
                    "The trip " + tripTitle + " has been updated",
                    actionUrl,
                    metadata(map, "tripId", "updatedBy"));
        }
    }

    void handlePaymentCompleted(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = text(map, "email");
            String userName = fallback(text(map, "userName", "displayName"), "Traveler");
            String userId = text(map, "userId");
            String amount = fallback(text(map, "amount"), "0.00");
            String planName = fallback(text(map, "planName", "plan"), "Trippy plan");
            String actionUrl = fallback(text(map, "actionUrl", "link"), "/dashboard/payments");

            log.info("Processing payment.completed for {}", email);
            sendTemplate(email, "Payment successful - " + amount + " EUR for " + planName,
                    "payment-success",
                    variables("userName", userName,
                            "amount", amount,
                            "planName", planName,
                            "dashboardUrl", actionUrl,
                            "link", actionUrl));

            createNotification(userId, NotificationType.PAYMENT_SUCCESS,
                    "Payment Successful",
                    "Your payment of " + amount + " EUR for " + planName + " was successful",
                    actionUrl,
                    metadata(map, "paymentId", "transactionId", "planName", "amount"));
        }
    }

    void handlePaymentFailed(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = text(map, "email");
            String userName = fallback(text(map, "userName", "displayName"), "Traveler");
            String userId = text(map, "userId");
            String actionUrl = fallback(text(map, "actionUrl", "link"), "/dashboard/payments");

            log.info("Processing payment.failed for {}", email);
            sendTemplate(email, "Payment could not be processed",
                    "payment-failed",
                    variables("userName", userName,
                            "dashboardUrl", actionUrl,
                            "link", actionUrl));

            createNotification(userId, NotificationType.PAYMENT_FAILED,
                    "Payment Failed",
                    "Your payment could not be processed. Please check your payment details.",
                    actionUrl,
                    metadata(map, "paymentId", "transactionId"));
        }
    }

    void handleItineraryGenerated(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = text(map, "email");
            String userName = fallback(text(map, "userName", "displayName"), "Traveler");
            String userId = text(map, "userId");
            String tripTitle = fallback(text(map, "tripTitle", "tripName", "title"), "your trip");
            String tripId = text(map, "tripId");
            String generationId = text(map, "generationId");
            String actionUrl = fallback(text(map, "actionUrl", "link"), tripUrl(tripId));

            log.info("Processing ai.itinerary.generated for {}", email);
            sendTemplate(email, "Your itinerary is ready",
                    "itinerary-ready",
                    variables("userName", userName,
                            "tripTitle", tripTitle,
                            "tripName", tripTitle,
                            "generationId", generationId,
                            "dashboardUrl", actionUrl,
                            "link", actionUrl));

            createNotification(userId, NotificationType.ITINERARY_READY,
                    "Itinerary Ready",
                    "Your itinerary for " + tripTitle + " is ready.",
                    actionUrl,
                    metadata(map, "tripId", "generationId"));
        }
    }

    private void sendTemplate(String email, String subject, String templateName,
                              Map<String, Object> variables) {
        if (email == null || email.isBlank()) {
            log.warn("Skipping {} email because recipient is missing", templateName);
            return;
        }
        emailService.sendTemplateEmail(email, subject, templateName, variables);
    }

    private void createNotification(String userId, NotificationType type, String title,
                                    String message, String actionUrl,
                                    Map<String, Object> metadata) {
        UUID parsedUserId = uuid(userId);
        if (parsedUserId == null) {
            return;
        }
        notificationService.createNotification(parsedUserId, type, title, message, actionUrl, metadata);
    }

    private String tripUrl(String tripId) {
        return tripId == null || tripId.isBlank()
                ? "/dashboard"
                : "/dashboard/trips/" + tripId;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String text(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }

    private Map<String, Object> variables(Object... keyValues) {
        Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object value = keyValues[i + 1];
            variables.put(keyValues[i].toString(), value == null ? "" : value);
        }
        return variables;
    }

    private Map<String, Object> metadata(Map<?, ?> map, String... keys) {
        Map<String, Object> metadata = new HashMap<>();
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                metadata.put(key, value);
            }
        }
        return metadata;
    }

    private UUID uuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping notification because user id is not a UUID: {}", value);
            return null;
        }
    }
}
