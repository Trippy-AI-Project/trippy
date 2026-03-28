package pse.trippy.chatservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import pse.trippy.chatservice.model.entity.ChatMessage;
import pse.trippy.chatservice.model.entity.ChatRoom;
import pse.trippy.chatservice.model.enums.MessageType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("ChatMessageRepository")
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    private ChatRoom savedRoom;

    @BeforeEach
    void setUp() {
        savedRoom = chatRoomRepository.save(ChatRoom.builder()
                .tripId(UUID.randomUUID())
                .build());

        for (int i = 0; i < 3; i++) {
            chatMessageRepository.save(ChatMessage.builder()
                    .chatRoom(savedRoom)
                    .senderId(UUID.randomUUID())
                    .senderDisplayName("User" + i)
                    .content("Message " + i)
                    .messageType(MessageType.TEXT)
                    .build());
        }
    }

    @Test
    @DisplayName("finds messages by room id ordered by createdAt desc")
    void findsMessagesByRoomId() {
        Page<ChatMessage> page = chatMessageRepository
                .findByChatRoomIdOrderByCreatedAtDesc(savedRoom.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("counts messages by room id")
    void countsMessagesByRoomId() {
        long count = chatMessageRepository.countByChatRoomId(savedRoom.getId());
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("returns empty page for unknown room")
    void returnsEmptyForUnknownRoom() {
        Page<ChatMessage> page = chatMessageRepository
                .findByChatRoomIdOrderByCreatedAtDesc(UUID.randomUUID(), PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }
}
