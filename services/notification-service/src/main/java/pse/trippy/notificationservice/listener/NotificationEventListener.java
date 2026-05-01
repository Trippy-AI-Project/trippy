package pse.trippy.notificationservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pse.trippy.notificationservice.model.enums.NotificationType;
import pse.trippy.notificationservice.service.EmailService;
import pse.trippy.notificationservice.service.NotificationService;

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
            case "trip.invitation.created" -> handleTripInvitation(payload);
            case "trip.invitation.accepted" -> handleInvitationAccepted(payload);
            case "trip.updated" -> handleTripUpdated(payload);
            case "payment.completed" -> handlePaymentCompleted(payload);
            case "payment.failed" -> handlePaymentFailed(payload);
            default -> log.warn("Unknown routing key: {}", routingKey);
        }
    }

    void handleUserRegistered(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = (String) map.get("email");
            String displayName = (String) map.get("displayName");
            String userId = (String) map.get("userId");
            String verificationToken = (String) map.get("verificationToken");

            log.info("Processing user.registered for {}", email);
            emailService.sendTemplateEmail(
                    email,
                    "Verify your Trippy account",
                    "email-verification",
                    Map.of("userName", displayName,
                            "verificationCode", verificationToken));

            if (userId != null && verificationToken != null) {
                notificationService.createNotification(
                        UUID.fromString(userId),
                        NotificationType.EMAIL_VERIFICATION,
                        "Verify your email",
                        "Use the verification code we emailed to activate your Trippy account.",
                        "/verify-email");
            }
        }
    }

    void handleUserEmailVerified(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = (String) map.get("email");
            String displayName = (String) map.get("displayName");
            String userId = (String) map.get("userId");
            String resolvedDisplayName =
                    (displayName != null && !displayName.isBlank()) ? displayName : "Traveler";

            log.info("Processing user.email.verified for {}", email);
            emailService.sendTemplateEmail(
                    email,
                    "Welcome to Trippy!",
                    "welcome",
                    Map.of("userName", resolvedDisplayName,
                            "dashboardUrl", DASHBOARD_URL));

            if (userId != null) {
                notificationService.createNotification(
                        UUID.fromString(userId),
                        NotificationType.WELCOME,
                        "Welcome to Trippy!",
                        "Your email is verified and your account is ready.",
                        DASHBOARD_URL);
            }
        }
    }

    void handleTripInvitation(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String inviteeEmail = (String) map.get("inviteeEmail");
            String inviteeName = (String) map.get("inviteeName");
            String inviterName = (String) map.get("inviterName");
            String tripTitle = (String) map.get("tripTitle");
            String inviteeId = (String) map.get("inviteeId");

            log.info("Processing trip.invitation.created for {}", inviteeEmail);
            emailService.sendTemplateEmail(
                    inviteeEmail,
                    inviterName + " invited you to " + tripTitle,
                    "trip-invitation",
                    Map.of("inviteeName", inviteeName,
                            "inviterName", inviterName,
                            "tripTitle", tripTitle,
                            "dashboardUrl", DASHBOARD_URL));

            if (inviteeId != null) {
                notificationService.createNotification(
                        UUID.fromString(inviteeId),
                        NotificationType.TRIP_INVITATION,
                        "Trip Invitation",
                        inviterName + " invited you to " + tripTitle,
                        DASHBOARD_URL);
            }
        }
    }

    void handleInvitationAccepted(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String inviterEmail = (String) map.get("inviterEmail");
            String inviterName = (String) map.get("inviterName");
            String inviterId = (String) map.get("inviterId");
            String inviteeName = (String) map.get("inviteeName");
            String tripTitle = (String) map.get("tripTitle");

            log.info("Processing trip.invitation.accepted for {}", inviterEmail);
            emailService.sendTemplateEmail(
                    inviterEmail,
                    inviteeName + " accepted your invitation to " + tripTitle,
                    "invitation-accepted",
                    Map.of("inviterName", inviterName,
                            "inviteeName", inviteeName,
                            "tripTitle", tripTitle,
                            "dashboardUrl", DASHBOARD_URL));

            if (inviterId != null) {
                notificationService.createNotification(
                        UUID.fromString(inviterId),
                        NotificationType.INVITATION_ACCEPTED,
                        "Invitation Accepted",
                        inviteeName + " accepted your invitation to " + tripTitle,
                        DASHBOARD_URL);
            }
        }
    }

    void handleTripUpdated(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = (String) map.get("email");
            String userName = (String) map.get("userName");
            String userId = (String) map.get("userId");
            String tripTitle = (String) map.get("tripTitle");
            String updatedBy = (String) map.get("updatedBy");

            log.info("Processing trip.updated for {}", email);
            emailService.sendTemplateEmail(
                    email,
                    "Trip updated: " + tripTitle,
                    "trip-updated",
                    Map.of("userName", userName,
                            "tripTitle", tripTitle,
                            "updatedBy", updatedBy != null ? updatedBy : "",
                            "dashboardUrl", DASHBOARD_URL));

            if (userId != null) {
                notificationService.createNotification(
                        UUID.fromString(userId),
                        NotificationType.TRIP_UPDATED,
                        "Trip Updated",
                        "The trip " + tripTitle + " has been updated",
                        DASHBOARD_URL);
            }
        }
    }

    void handlePaymentCompleted(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = (String) map.get("email");
            String userName = (String) map.get("userName");
            String userId = (String) map.get("userId");
            String amount = (String) map.get("amount");
            String planName = (String) map.get("planName");

            log.info("Processing payment.completed for {}", email);
            emailService.sendTemplateEmail(
                    email,
                    "Payment successful — " + amount + " EUR for " + planName,
                    "payment-success",
                    Map.of("userName", userName,
                            "amount", amount,
                            "planName", planName,
                            "dashboardUrl", DASHBOARD_URL));

            if (userId != null) {
                notificationService.createNotification(
                        UUID.fromString(userId),
                        NotificationType.PAYMENT_SUCCESS,
                        "Payment Successful",
                        "Your payment of " + amount + " EUR for " + planName + " was successful",
                        DASHBOARD_URL);
            }
        }
    }

    void handlePaymentFailed(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = (String) map.get("email");
            String userName = (String) map.get("userName");
            String userId = (String) map.get("userId");

            log.info("Processing payment.failed for {}", email);
            emailService.sendTemplateEmail(
                    email,
                    "Payment could not be processed",
                    "payment-failed",
                    Map.of("userName", userName,
                            "dashboardUrl", DASHBOARD_URL));

            if (userId != null) {
                notificationService.createNotification(
                        UUID.fromString(userId),
                        NotificationType.PAYMENT_FAILED,
                        "Payment Failed",
                        "Your payment could not be processed. Please check your payment details.",
                        DASHBOARD_URL);
            }
        }
    }
}
