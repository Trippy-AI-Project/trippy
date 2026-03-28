package pse.trippy.chatservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.chatservice.dto.request.SendMessageRequest;
import pse.trippy.chatservice.dto.response.ChatMessageResponse;
import pse.trippy.chatservice.model.enums.MessageType;
import pse.trippy.chatservice.service.ChatMessageService;

import java.util.UUID;

/**
 * HTTP fallback controller for sending chat messages
 * (for clients that can't use WebSocket).
 */
@RestController
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @PostMapping("/trips/{tripId}/chat/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable UUID tripId,
            @RequestBody @Valid SendMessageRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-DisplayName", required = false) String displayName) {

        UUID senderId = userId != null ? UUID.fromString(userId) : UUID.randomUUID();
        String senderName = displayName != null ? displayName : "Anonymous";

        MessageType messageType;
        try {
            messageType = MessageType.valueOf(request.getType());
        } catch (IllegalArgumentException e) {
            messageType = MessageType.TEXT;
        }

        ChatMessageResponse response = chatMessageService.sendMessage(
                tripId, senderId, senderName, request.getContent(), messageType);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
