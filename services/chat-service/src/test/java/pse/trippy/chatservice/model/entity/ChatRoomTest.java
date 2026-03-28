package pse.trippy.chatservice.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatRoom Entity")
class ChatRoomTest {

    @Test
    @DisplayName("builds with defaults and sets createdAt on prePersist")
    void buildsWithDefaultsAndSetsTimestamp() {
        UUID tripId = UUID.randomUUID();
        ChatRoom room = ChatRoom.builder()
                .tripId(tripId)
                .build();

        room.prePersist();

        assertThat(room.getTripId()).isEqualTo(tripId);
        assertThat(room.getCreatedAt()).isNotNull();
    }
}
