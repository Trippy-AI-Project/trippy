package pse.trippy.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.chatservice.dto.response.ChatMessageResponse;
import pse.trippy.chatservice.model.entity.ChatMessage;
import pse.trippy.chatservice.model.entity.ChatRoom;
import pse.trippy.chatservice.model.enums.MessageType;
import pse.trippy.chatservice.repository.ChatMessageRepository;

import java.util.UUID;

/**
 * Service for persisting and broadcasting chat messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomService chatRoomService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Persists a message and broadcasts it to the STOMP topic.
     */
    @Transactional
    public ChatMessageResponse sendMessage(UUID tripId, UUID senderId,
                                           String senderDisplayName, String content,
                                           MessageType messageType) {

        ChatRoom room = chatRoomService.getRoomByTripId(tripId);

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .senderId(senderId)
                .senderDisplayName(senderDisplayName)
                .content(content)
                .messageType(messageType)
                .build());

        log.info("Message {} persisted in room {} for trip {}",
                message.getId(), room.getId(), tripId);

        ChatMessageResponse response = toResponse(message);

        // Broadcast to STOMP subscribers
        messagingTemplate.convertAndSend(
                "/topic/trips/" + tripId + "/messages", response);

        return response;
    }

    private ChatMessageResponse toResponse(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .senderId(msg.getSenderId())
                .senderDisplayName(msg.getSenderDisplayName())
                .content(msg.isDeleted() ? "This message was deleted" : msg.getContent())
                .type(msg.getMessageType().name())
                .createdAt(msg.getCreatedAt())
                .edited(msg.getEditedAt() != null)
                .build();
    }
}
