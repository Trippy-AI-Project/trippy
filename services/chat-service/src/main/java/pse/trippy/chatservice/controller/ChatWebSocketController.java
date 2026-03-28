package pse.trippy.chatservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import pse.trippy.chatservice.dto.request.SendMessageRequest;
import pse.trippy.chatservice.dto.response.ChatMessageResponse;
import pse.trippy.chatservice.model.entity.ChatMessage;
import pse.trippy.chatservice.model.entity.ChatRoom;
import pse.trippy.chatservice.model.enums.MessageType;
import pse.trippy.chatservice.repository.ChatMessageRepository;
import pse.trippy.chatservice.service.ChatRoomService;

import java.util.UUID;

/**
 * STOMP WebSocket controller for real-time chat messaging.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * Handles messages sent to /app/trips/{tripId}/send.
     * Persists the message and broadcasts to /topic/trips/{tripId}/messages.
     */
    @MessageMapping("/trips/{tripId}/send")
    @SendTo("/topic/trips/{tripId}/messages")
    public ChatMessageResponse sendMessage(
            @DestinationVariable UUID tripId,
            SendMessageRequest request) {

        log.info("Received message for trip {}: {}", tripId, request.getContent());

        ChatRoom room = chatRoomService.getRoomByTripId(tripId);

        // For now, use a placeholder sender until JWT auth is integrated
        UUID senderId = UUID.randomUUID();
        String senderName = "Anonymous";

        MessageType messageType;
        try {
            messageType = MessageType.valueOf(request.getType());
        } catch (IllegalArgumentException e) {
            messageType = MessageType.TEXT;
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .senderId(senderId)
                .senderDisplayName(senderName)
                .content(request.getContent())
                .messageType(messageType)
                .build());

        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .senderDisplayName(message.getSenderDisplayName())
                .content(message.getContent())
                .type(message.getMessageType().name())
                .createdAt(message.getCreatedAt())
                .edited(false)
                .build();
    }
}
