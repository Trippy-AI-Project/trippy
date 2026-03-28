package pse.trippy.chatservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
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
     */
    @MessageMapping("/trips/{tripId}/send")
    @SendTo("/topic/trips/{tripId}/messages")
    public ChatMessageResponse sendMessage(
            @DestinationVariable UUID tripId,
            SendMessageRequest request) {

        log.info("Received STOMP message for trip {}: {}", tripId, request.getContent());

        // Placeholder sender until JWT auth is integrated on WebSocket handshake
        UUID senderId = UUID.randomUUID();
        String senderName = "Anonymous";

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
