package pse.trippy.chatservice.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pse.trippy.chatservice.model.enums.MessageType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatMessage Entity")
class ChatMessageTest {

    @Test
    @DisplayName("builds with defaults and sets createdAt on prePersist")
    void buildsWithDefaultsAndSetsTimestamp() {
        ChatRoom room = ChatRoom.builder().tripId(UUID.randomUUID()).build();

        ChatMessage msg = ChatMessage.builder()
                .chatRoom(room)
                .senderId(UUID.randomUUID())
                .senderDisplayName("Alice")
                .content("Hello!")
                .build();

        msg.prePersist();

        assertThat(msg.getMessageType()).isEqualTo(MessageType.TEXT);
        assertThat(msg.isDeleted()).isFalse();
        assertThat(msg.getCreatedAt()).isNotNull();
        assertThat(msg.getEditedAt()).isNull();
    }

    @Test
    @DisplayName("supports all message types")
    void supportsAllMessageTypes() {
        ChatMessage msg = ChatMessage.builder()
                .chatRoom(ChatRoom.builder().tripId(UUID.randomUUID()).build())
                .senderId(UUID.randomUUID())
                .senderDisplayName("Bot")
                .content("system event")
                .messageType(MessageType.SYSTEM)
                .build();

        assertThat(msg.getMessageType()).isEqualTo(MessageType.SYSTEM);
    }
}
