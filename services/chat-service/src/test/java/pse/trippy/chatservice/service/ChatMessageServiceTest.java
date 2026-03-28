package pse.trippy.chatservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import pse.trippy.chatservice.dto.response.ChatMessageResponse;
import pse.trippy.chatservice.model.entity.ChatMessage;
import pse.trippy.chatservice.model.entity.ChatRoom;
import pse.trippy.chatservice.model.enums.MessageType;
import pse.trippy.chatservice.repository.ChatMessageRepository;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageService")
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomService chatRoomService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("sendMessage persists and broadcasts")
    void sendMessagePersistsAndBroadcasts() {
        UUID tripId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();
        String content = "Hello everyone!";

        ChatRoom room = ChatRoom.builder().id(roomId).tripId(tripId).createdAt(Instant.now()).build();
        when(chatRoomService.getRoomByTripId(tripId)).thenReturn(room);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(msgId);
            m.prePersist();
            return m;
        });

        ChatMessageResponse response = chatMessageService.sendMessage(
                tripId, senderId, "Alice", content, MessageType.TEXT);

        // Verify persistence
        assertThat(response.getId()).isEqualTo(msgId);
        assertThat(response.getSenderId()).isEqualTo(senderId);
        assertThat(response.getSenderDisplayName()).isEqualTo("Alice");
        assertThat(response.getContent()).isEqualTo(content);
        assertThat(response.getType()).isEqualTo("TEXT");

        // Verify saved entity
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        ChatMessage saved = captor.getValue();
        assertThat(saved.getChatRoom().getId()).isEqualTo(roomId);
        assertThat(saved.getSenderId()).isEqualTo(senderId);
        assertThat(saved.getContent()).isEqualTo(content);

        // Verify broadcast
        verify(messagingTemplate).convertAndSend(
                eq("/topic/trips/" + tripId + "/messages"),
                any(ChatMessageResponse.class));
    }

    @Test
    @DisplayName("deleted messages return placeholder content")
    void deletedMessagesReturnPlaceholder() {
        UUID tripId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        ChatRoom room = ChatRoom.builder().id(UUID.randomUUID()).tripId(tripId).createdAt(Instant.now()).build();

        when(chatRoomService.getRoomByTripId(tripId)).thenReturn(room);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setDeleted(true);
            m.prePersist();
            return m;
        });

        ChatMessageResponse response = chatMessageService.sendMessage(
                tripId, senderId, "Bob", "secret", MessageType.TEXT);

        assertThat(response.getContent()).isEqualTo("This message was deleted");
    }
}
