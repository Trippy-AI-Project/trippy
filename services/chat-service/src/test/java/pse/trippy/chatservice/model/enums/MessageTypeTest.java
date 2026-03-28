package pse.trippy.chatservice.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessageType Enum")
class MessageTypeTest {

    @Test
    @DisplayName("has exactly 4 values")
    void hasFourValues() {
        assertThat(MessageType.values()).hasSize(4);
    }

    @Test
    @DisplayName("contains TEXT, IMAGE, FILE, SYSTEM")
    void containsExpectedValues() {
        assertThat(MessageType.valueOf("TEXT")).isEqualTo(MessageType.TEXT);
        assertThat(MessageType.valueOf("IMAGE")).isEqualTo(MessageType.IMAGE);
        assertThat(MessageType.valueOf("FILE")).isEqualTo(MessageType.FILE);
        assertThat(MessageType.valueOf("SYSTEM")).isEqualTo(MessageType.SYSTEM);
    }
}
