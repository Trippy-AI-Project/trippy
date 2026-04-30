package pse.trippy.notificationservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import pse.trippy.notificationservice.dto.event.TripInvitationEvent;
import pse.trippy.notificationservice.dto.event.UserRegisteredEvent;
import pse.trippy.notificationservice.service.EmailService;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private static final String DASHBOARD_URL = "https://trippy.app/dashboard";

    private final EmailService emailService;

    @RabbitListener(queues = "notification.events",
            messageConverter = "jsonMessageConverter")
    public void handleEvent(Object payload,
                            @Header("amqp_receivedRoutingKey") String routingKey) {
        log.info("Received event with routing key: {}", routingKey);

        switch (routingKey) {
            case "user.registered" -> handleUserRegistered(payload);
            case "trip.invitation.created" -> handleTripInvitation(payload);
            default -> log.warn("Unknown routing key: {}", routingKey);
        }
    }

    void handleUserRegistered(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String email = (String) map.get("email");
            String displayName = (String) map.get("displayName");

            log.info("Processing user.registered for {}", email);
            emailService.sendTemplateEmail(
                    email,
                    "Welcome to Trippy!",
                    "welcome",
                    Map.of("userName", displayName,
                            "dashboardUrl", DASHBOARD_URL));
        }
    }

    void handleTripInvitation(Object payload) {
        if (payload instanceof Map<?, ?> map) {
            String inviteeEmail = (String) map.get("inviteeEmail");
            String inviteeName = (String) map.get("inviteeName");
            String inviterName = (String) map.get("inviterName");
            String tripTitle = (String) map.get("tripTitle");

            log.info("Processing trip.invitation.created for {}", inviteeEmail);
            emailService.sendTemplateEmail(
                    inviteeEmail,
                    inviterName + " invited you to " + tripTitle,
                    "trip-invitation",
                    Map.of("inviteeName", inviteeName,
                            "inviterName", inviterName,
                            "tripTitle", tripTitle,
                            "dashboardUrl", DASHBOARD_URL));
        }
    }
}
