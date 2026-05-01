package pse.trippy.chatservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import pse.trippy.chatservice.model.enums.MessageType;
import pse.trippy.chatservice.service.ChatMessageService;
import pse.trippy.chatservice.service.ChatPresenceService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for WebSocket session disconnect events to remove users from chat presence
 * and broadcast system leave messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketDisconnectListener {

    private final ChatPresenceService chatPresenceService;
    private final ChatMessageService chatMessageService;

    /**
     * Stores session-to-user/trip mapping set by the channel interceptor on subscribe.
     * Key: sessionId, Value: map of tripId -> userId
     */
    private static final Map<String, Map<UUID, UUID>> sessionSubscriptions = new ConcurrentHashMap<>();

    /**
     * Stores session-to-display-name mapping.
     */
    private static final Map<String, String> sessionDisplayNames = new ConcurrentHashMap<>();

    /**
     * Called by the channel interceptor when a user subscribes to a trip chat.
     */
    public void trackSubscription(String sessionId, UUID tripId, UUID userId, String displayName) {
        sessionSubscriptions.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                .put(tripId, userId);
        if (displayName != null && !displayName.isBlank()) {
            sessionDisplayNames.put(sessionId, displayName);
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) {
            return;
        }

        Map<UUID, UUID> subscriptions = sessionSubscriptions.remove(sessionId);
        String displayName = sessionDisplayNames.remove(sessionId);

        if (subscriptions == null || subscriptions.isEmpty()) {
            return;
        }

        String name = (displayName != null && !displayName.isBlank()) ? displayName : "A user";

        subscriptions.forEach((tripId, userId) -> {
            chatPresenceService.removeUser(tripId, userId);
            try {
                chatMessageService.sendMessage(
                        tripId, userId, "System",
                        name + " left the chat",
                        MessageType.SYSTEM);
            } catch (Exception e) {
                log.warn("Failed to send leave message for user {} in trip {}: {}",
                        userId, tripId, e.getMessage());
            }
            log.info("User {} disconnected from trip {} chat", userId, tripId);
        });
    }
}
