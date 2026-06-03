package pse.trippy.chatservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import pse.trippy.chatservice.exception.MessageNotFoundException;
import pse.trippy.chatservice.model.entity.ChatMessage;
import pse.trippy.chatservice.repository.ChatMessageRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ModerationService")
class ModerationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    private ModerationService moderationService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        moderationService = new ModerationService(redisTemplate, chatMessageRepository);
    }

    // -------------------------------------------------------------------- ban

    @Test
    @DisplayName("banUser: writes Redis key with Duration TTL")
    void banUser_writesRedisKey() {
        moderationService.banUser(userId, 60);

        verify(valueOps).set(
                eq("moderation:ban:" + userId),
                anyString(),
                eq(Duration.ofMinutes(60)));
    }

    @Test
    @DisplayName("banUser: 0 minutes uses MAX_DURATION (30 days)")
    void banUser_zeroDuration_usesMax() {
        moderationService.banUser(userId, 0);

        verify(valueOps).set(
                eq("moderation:ban:" + userId),
                anyString(),
                eq(Duration.ofDays(30)));
    }

    @Test
    @DisplayName("unbanUser: deletes Redis key")
    void unbanUser_deletesKey() {
        moderationService.unbanUser(userId);

        verify(redisTemplate).delete("moderation:ban:" + userId);
    }

    @Test
    @DisplayName("isBanned: returns true when key exists")
    void isBanned_keyExists_returnsTrue() {
        when(redisTemplate.hasKey("moderation:ban:" + userId)).thenReturn(true);

        assertThat(moderationService.isBanned(userId)).isTrue();
    }

    @Test
    @DisplayName("isBanned: returns false when key absent")
    void isBanned_keyAbsent_returnsFalse() {
        when(redisTemplate.hasKey("moderation:ban:" + userId)).thenReturn(false);

        assertThat(moderationService.isBanned(userId)).isFalse();
    }

    @Test
    @DisplayName("banUser: negative duration is rejected")
    void banUser_negativeDuration_rejected() {
        assertThatThrownBy(() -> moderationService.banUser(userId, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMinutes must be >= 0");
    }

    // ------------------------------------------------------------------- mute

    @Test
    @DisplayName("muteUser: writes Redis key with Duration TTL")
    void muteUser_writesRedisKey() {
        moderationService.muteUser(userId, 30);

        verify(valueOps).set(
                eq("moderation:mute:" + userId),
                anyString(),
                eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("unmuteUser: deletes Redis key")
    void unmuteUser_deletesKey() {
        moderationService.unmuteUser(userId);

        verify(redisTemplate).delete("moderation:mute:" + userId);
    }

    @Test
    @DisplayName("isMuted: returns true when key exists")
    void isMuted_keyExists_returnsTrue() {
        when(redisTemplate.hasKey("moderation:mute:" + userId)).thenReturn(true);

        assertThat(moderationService.isMuted(userId)).isTrue();
    }

    @Test
    @DisplayName("muteUser: negative duration is rejected")
    void muteUser_negativeDuration_rejected() {
        assertThatThrownBy(() -> moderationService.muteUser(userId, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMinutes must be >= 0");
    }

    // ---------------------------------------------------------- deleteMessage

    @Test
    @DisplayName("deleteMessage: marks message as deleted")
    void deleteMessage_marksDeleted() {
        UUID messageId = UUID.randomUUID();
        ChatMessage message = new ChatMessage();

        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.of(message));

        moderationService.deleteMessage(messageId);

        assertThat(message.isDeleted()).isTrue();
        verify(chatMessageRepository).save(message);
    }

    @Test
    @DisplayName("deleteMessage: throws 404 when message not found")
    void deleteMessage_notFound_throws404() {
        UUID messageId = UUID.randomUUID();
        when(chatMessageRepository.findById(messageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moderationService.deleteMessage(messageId))
                .isInstanceOf(MessageNotFoundException.class);
    }
}
