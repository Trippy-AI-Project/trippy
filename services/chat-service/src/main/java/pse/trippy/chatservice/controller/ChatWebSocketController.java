package pse.trippy.chatservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import pse.trippy.chatservice.dto.request.SendMessageRequest;
import pse.trippy.chatservice.dto.response.ChatMessageResponse;
import pse.trippy.chatservice.model.enums.MessageType;
import pse.trippy.chatservice.service.ChatMessageService;

import java.util.UUID;

/**
 * STOMP WebSocket controller for real-time chat messaging.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatMessageService chatMessageService;

    /**
     * Handles messages sent to /app/trips/{tripId}/send.
     * Persists the message and broadcasts to /topic/trips/{tripId}/messages.
     * User identity is extracted from STOMP headers set by the API gateway.
     */
    @MessageMapping("/trips/{tripId}/send")
    @SendTo("/topic/trips/{tripId}/messages")
    public ChatMessageResponse sendMessage(
            @DestinationVariable UUID tripId,
            @Header(value = "X-User-Id", defaultValue = "") String userIdHeader,
            @Header(value = "X-User-DisplayName", defaultValue = "Anonymous") String displayName,
            SendMessageRequest request) {

        log.info("Received STOMP message for trip {}: {}", tripId, request.getContent());

        UUID senderId;
        try {
            senderId = UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            senderId = UUID.randomUUID();
        }

        String senderName = (displayName != null && !displayName.isBlank()) ? displayName : "Anonymous";

        MessageType messageType;
        try {
            messageType = MessageType.valueOf(request.getType());
        } catch (IllegalArgumentException e) {
            messageType = MessageType.TEXT;
        }

        return chatMessageService.sendMessage(
                tripId, senderId, senderName, request.getContent(), messageType);
    }
}
