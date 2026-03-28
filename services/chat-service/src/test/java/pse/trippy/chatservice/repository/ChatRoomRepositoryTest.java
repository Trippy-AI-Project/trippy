package pse.trippy.chatservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pse.trippy.chatservice.model.entity.ChatRoom;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("ChatRoomRepository")
class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    private ChatRoom savedRoom;

    @BeforeEach
    void setUp() {
        savedRoom = chatRoomRepository.save(ChatRoom.builder()
                .tripId(UUID.randomUUID())
                .build());
    }

    @Test
    @DisplayName("findByTripId returns room when exists")
    void findByTripIdReturnsRoom() {
        Optional<ChatRoom> result = chatRoomRepository.findByTripId(savedRoom.getTripId());
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(savedRoom.getId());
    }

    @Test
    @DisplayName("findByTripId returns empty for unknown trip")
    void findByTripIdReturnsEmpty() {
        Optional<ChatRoom> result = chatRoomRepository.findByTripId(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsByTripId returns true when exists")
    void existsByTripIdReturnsTrue() {
        assertThat(chatRoomRepository.existsByTripId(savedRoom.getTripId())).isTrue();
    }

    @Test
    @DisplayName("existsByTripId returns false when not exists")
    void existsByTripIdReturnsFalse() {
        assertThat(chatRoomRepository.existsByTripId(UUID.randomUUID())).isFalse();
    }
}
