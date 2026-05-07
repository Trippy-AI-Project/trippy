package pse.trippy.userservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pse.trippy.userservice.config.RabbitMqConfig;
import pse.trippy.userservice.service.UserService;

import java.util.Map;
import java.util.UUID;

/**
 * Listens for events from trip-service and reacts accordingly.
 *
 * <p>On {@code trip.created}: upgrades the trip creator's platform role to HOST
 * so that their next JWT access token will reflect the new role.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TripEventListener {

    private final UserService userService;

    /**
     * Handles a {@code trip.created} event published by trip-service.
     *
     * <p>Extracts {@code createdBy} (the creator's userId) and promotes
     * their role from MEMBER → HOST in the user-service database.
     * The change is reflected in the user's next JWT access token
     * (issued on the next login or token refresh).
     *
     * @param event the deserialized event payload
     */
    @RabbitListener(queues = RabbitMqConfig.TRIP_CREATED_QUEUE)
    public void onTripCreated(Map<String, Object> event) {
        try {
            String createdBy = (String) event.get("createdBy");
            if (createdBy == null) {
                log.warn("trip.created event missing 'createdBy' field: {}", event);
                return;
            }
            UUID userId = UUID.fromString(createdBy);
            userService.upgradeToHost(userId);
            log.info("Processed trip.created event: userId={} promoted to HOST", userId);
        } catch (Exception ex) {
            log.error("Failed to process trip.created event: {}", event, ex);
        }
    }
}
