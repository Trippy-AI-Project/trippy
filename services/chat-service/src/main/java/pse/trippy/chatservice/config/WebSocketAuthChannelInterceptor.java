package pse.trippy.chatservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import pse.trippy.chatservice.client.TripServiceClient;
import pse.trippy.chatservice.model.enums.MessageType;
import pse.trippy.chatservice.service.ChatMessageService;
import pse.trippy.chatservice.service.ChatPresenceService;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Channel interceptor that verifies trip participation on SUBSCRIBE
 * to chat topics and broadcasts system join messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TOPIC_PATTERN =
            Pattern.compile("^/topic/trips/([0-9a-fA-F\\-]+)/messages$");

    private final TripServiceClient tripServiceClient;
    private final ChatPresenceService chatPresenceService;
    private final ChatMessageService chatMessageService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();

        if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher matcher = TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        UUID tripId = UUID.fromString(matcher.group(1));
        String userIdHeader = accessor.getFirstNativeHeader("X-User-Id");
        String displayName = accessor.getFirstNativeHeader("X-User-DisplayName");

        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new MessageDeliveryException("Missing X-User-Id header");
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            throw new MessageDeliveryException("Invalid X-User-Id header");
        }

        if (!tripServiceClient.isParticipant(tripId, userId)) {
            log.warn("User {} denied subscription to trip {} - not a participant", userId, tripId);
            throw new MessageDeliveryException(
                    "User is not a participant of trip " + tripId);
        }

        log.info("User {} verified as participant for trip {}", userId, tripId);

        // Track presence and broadcast join message if new
        boolean newJoin = chatPresenceService.addUser(tripId, userId);
        if (newJoin) {
            String name = (displayName != null && !displayName.isBlank()) ? displayName : "A user";
            try {
                chatMessageService.sendMessage(
                        tripId, userId, "System",
                        name + " joined the chat",
                        MessageType.SYSTEM);
            } catch (Exception e) {
                log.warn("Failed to send join message for user {} in trip {}: {}",
                        userId, tripId, e.getMessage());
            }
        }
    }
}
