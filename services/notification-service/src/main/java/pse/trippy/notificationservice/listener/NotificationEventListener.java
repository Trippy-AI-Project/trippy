package pse.trippy.notificationservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pse.trippy.notificationservice.model.enums.NotificationType;
import pse.trippy.notificationservice.service.EmailService;
import pse.trippy.notificationservice.service.NotificationService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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
            case "trip.invitation.created" -> handleTripInvitation(payload);
            case "trip.invitation.accepted", "trip.joined" -> handleTripJoined(payload);
            case "trip.updated" -> handleTripUpdated(payload);
            case "payment.completed" -> handlePaymentCompleted(payload);
            case "payment.failed" -> handlePaymentFailed(payload);
            case "ai.itinerary.ready", "itinerary.ready" -> handleItineraryReady(payload);
            case "system.notification" -> handleSystemNotification(payload);
            default -> log.warn("Unknown routing key: {}", routingKey);
        }
    }

    void handleUserRegistered(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = stringValue(map, "email");
            String displayName = defaultString(stringValue(map, "displayName"), "Traveler");
            String verificationToken = stringValue(map, "verificationToken");

            log.info("Processing user.registered for {}", email);
            sendTemplateEmail(
                    email,
                    "Verify your Trippy account",
                    "email-verification",
                    Map.of("userName", displayName,
                            "verificationCode", defaultString(verificationToken, "")));

            Optional<UUID> userId = uuidValue(map, "userId");
            if (userId.isPresent() && verificationToken != null) {
                notificationService.createNotification(
                        userId.get(),
                        NotificationType.EMAIL_VERIFICATION,
                        "Verify your email",
                        "Use the verification code we emailed to activate your Trippy account.",
                        "/verify-email",
                        metadata(map, "userId"));
            }
        }
    }

    void handleUserEmailVerified(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = stringValue(map, "email");
            String displayName = defaultString(stringValue(map, "displayName"), "Traveler");

            log.info("Processing user.email.verified for {}", email);
            sendTemplateEmail(
                    email,
                    "Welcome to Trippy!",
                    "welcome",
                    Map.of("userName", displayName,
                            "dashboardUrl", DASHBOARD_URL));

            uuidValue(map, "userId").ifPresent(userId -> notificationService.createNotification(
                    userId,
                    NotificationType.WELCOME,
                    "Welcome to Trippy!",
                    "Your email is verified and your account is ready.",
                    DASHBOARD_URL,
                    metadata(map, "userId")));
        }
    }

    void handleTripInvitation(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String inviteeEmail = stringValue(map, "inviteeEmail");
            String inviteeName = defaultString(stringValue(map, "inviteeName"), "Traveler");
            String inviterName = defaultString(stringValue(map, "inviterName"), "Someone");
            String tripTitle = defaultString(stringValue(map, "tripTitle"), "your trip");

            log.info("Processing trip.invitation.created for {}", inviteeEmail);
            sendTemplateEmail(
                    inviteeEmail,
                    inviterName + " invited you to " + tripTitle,
                    "trip-invitation",
                    Map.of("inviteeName", inviteeName,
                            "inviterName", inviterName,
                            "tripTitle", tripTitle,
                            "dashboardUrl", DASHBOARD_URL));

            uuidValue(map, "inviteeId", "inviteeUserId", "userId").ifPresent(inviteeId ->
                    notificationService.createNotification(
                            inviteeId,
                            NotificationType.TRIP_INVITE,
                            "Trip Invitation",
                            inviterName + " invited you to " + tripTitle,
                            tripActionUrl(map),
                            metadata(map, "inviteeId", "inviteeUserId", "userId")));
        }
    }

    void handleInvitationAccepted(Object payload) {
        handleTripJoined(payload);
    }

    void handleTripJoined(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String inviterEmail = stringValue(map, "inviterEmail");
            String inviterName = defaultString(stringValue(map, "inviterName"), "Traveler");
            String inviteeName = defaultString(stringValue(map, "inviteeName"), "Someone");
            String tripTitle = defaultString(stringValue(map, "tripTitle"), "your trip");

            log.info("Processing trip.joined for {}", inviterEmail);
            sendTemplateEmail(
                    inviterEmail,
                    inviteeName + " joined " + tripTitle,
                    "invitation-accepted",
                    Map.of("inviterName", inviterName,
                            "inviteeName", inviteeName,
                            "tripTitle", tripTitle,
                            "dashboardUrl", DASHBOARD_URL));

            uuidValue(map, "inviterId", "userId", "recipientUserId").ifPresent(inviterId ->
                    notificationService.createNotification(
                            inviterId,
                            NotificationType.TRIP_JOINED,
                            "Traveler Joined",
                            inviteeName + " joined " + tripTitle,
                            tripActionUrl(map),
                            metadata(map, "inviterId", "userId", "recipientUserId")));
        }
    }

    void handleTripUpdated(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = stringValue(map, "email");
            String userName = defaultString(stringValue(map, "userName"), "Traveler");
            String tripTitle = defaultString(stringValue(map, "tripTitle"), "your trip");
            String updatedBy = defaultString(stringValue(map, "updatedBy"), "");

            log.info("Processing trip.updated for {}", email);
            sendTemplateEmail(
                    email,
                    "Trip updated: " + tripTitle,
                    "trip-updated",
                    Map.of("userName", userName,
                            "tripTitle", tripTitle,
                            "updatedBy", updatedBy,
                            "dashboardUrl", DASHBOARD_URL));

            uuidValue(map, "userId").ifPresent(userId -> notificationService.createNotification(
                    userId,
                    NotificationType.TRIP_UPDATED,
                    "Trip Updated",
                    "The trip " + tripTitle + " has been updated",
                    tripActionUrl(map),
                    metadata(map, "userId")));
        }
    }

    void handlePaymentCompleted(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = stringValue(map, "email");
            String userName = defaultString(stringValue(map, "userName"), "Traveler");
            String amount = defaultString(stringValue(map, "amount"), "");
            String planName = defaultString(stringValue(map, "planName"), "Trippy");

            log.info("Processing payment.completed for {}", email);
            sendTemplateEmail(
                    email,
                    "Payment successful - " + amount + " EUR for " + planName,
                    "payment-success",
                    Map.of("userName", userName,
                            "amount", amount,
                            "planName", planName,
                            "dashboardUrl", DASHBOARD_URL));

            uuidValue(map, "userId").ifPresent(userId -> notificationService.createNotification(
                    userId,
                    NotificationType.PAYMENT_SUCCESS,
                    "Payment Successful",
                    "Your payment of " + amount + " EUR for " + planName + " was successful",
                    DASHBOARD_URL,
                    metadata(map, "userId")));
        }
    }

    void handlePaymentFailed(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = stringValue(map, "email");
            String userName = defaultString(stringValue(map, "userName"), "Traveler");

            log.info("Processing payment.failed for {}", email);
            sendTemplateEmail(
                    email,
                    "Payment could not be processed",
                    "payment-failed",
                    Map.of("userName", userName,
                            "dashboardUrl", DASHBOARD_URL));

            uuidValue(map, "userId").ifPresent(userId -> notificationService.createNotification(
                    userId,
                    NotificationType.PAYMENT_FAILED,
                    "Payment Failed",
                    "Your payment could not be processed. Please check your payment details.",
                    DASHBOARD_URL,
                    metadata(map, "userId")));
        }
    }

    void handleItineraryReady(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String tripTitle = defaultString(stringValue(map, "tripTitle"), "your trip");
            String tripUrl = tripActionUrl(map);

            sendTemplateEmail(
                    stringValue(map, "email"),
                    "Your Trippy itinerary is ready",
                    "itinerary-ready",
                    Map.of("userName", defaultString(stringValue(map, "userName"), "Traveler"),
                            "tripTitle", tripTitle,
                            "destination", defaultString(stringValue(map, "destination"), ""),
                            "tripUrl", tripUrl));

            uuidValue(map, "userId", "ownerId", "recipientUserId").ifPresent(userId ->
                    notificationService.createNotification(
                            userId,
                            NotificationType.ITINERARY_READY,
                            "Itinerary Ready",
                            "Your itinerary for " + tripTitle + " is ready to review.",
                            tripUrl,
                            metadata(map, "userId", "ownerId", "recipientUserId")));
        }
    }

    void handleSystemNotification(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String title = defaultString(stringValue(map, "title"), "Trippy update");
            String message = defaultString(stringValue(map, "message"), "You have a new Trippy notification.");
            String actionUrl = defaultString(stringValue(map, "actionUrl"), DASHBOARD_URL);

            sendTemplateEmail(
                    stringValue(map, "email"),
                    title,
                    "system-notification",
                    Map.of("userName", defaultString(stringValue(map, "userName"), "Traveler"),
                            "title", title,
                            "message", message,
                            "actionUrl", actionUrl));

            uuidValue(map, "userId", "recipientUserId").ifPresent(userId ->
                    notificationService.createNotification(
                            userId,
                            NotificationType.SYSTEM,
                            title,
                            message,
                            actionUrl,
                            metadata(map, "userId", "recipientUserId")));
        }
    }

    private void sendTemplateEmail(String to, String subject, String templateName,
                                   Map<String, Object> variables) {
        if (to == null || to.isBlank()) {
            log.debug("Skipping {} email because no recipient was supplied", templateName);
            return;
        }
        emailService.sendTemplateEmail(to, subject, templateName, variables);
    }

    private Optional<UUID> uuidValue(Map<?, ?> map, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Object value = map.get(fieldName);
            if (value == null) {
                continue;
            }
            try {
                return Optional.of(UUID.fromString(value.toString()));
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping notification event with malformed UUID in {}: {}", fieldName, value);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private String stringValue(Map<?, ?> map, String fieldName) {
        Object value = map.get(fieldName);
        return value != null ? value.toString() : null;
    }

    private String defaultString(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private String tripActionUrl(Map<?, ?> map) {
        String tripId = stringValue(map, "tripId");
        return tripId != null && !tripId.isBlank()
                ? "/dashboard/trips/" + tripId
                : DASHBOARD_URL;
    }

    private Map<String, Object> metadata(Map<?, ?> map, String... userIdKeys) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                metadata.put(entry.getKey().toString(), entry.getValue());
            }
        }
        for (String userIdKey : userIdKeys) {
            metadata.remove(userIdKey);
        }
        return metadata.isEmpty() ? null : metadata;
    }
}
