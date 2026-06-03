package pse.trippy.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.chatservice.exception.MessageNotFoundException;
import pse.trippy.chatservice.model.entity.ChatMessage;
import pse.trippy.chatservice.repository.ChatMessageRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for ticket 4.4 — Chat Moderation API.
 *
 * <p>Ban/mute state is stored in Redis with a configurable expiry so actions
 * automatically lift without a separate "unban" call.  Permanent actions can
 * be expressed with {@link #MAX_DURATION}.
 *
 * <p>Redis key patterns:
 * <ul>
 *   <li>{@code moderation:ban:{userId}}  — value is the expiry {@link Instant}</li>
 *   <li>{@code moderation:mute:{userId}} — value is the expiry {@link Instant}</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {

    /** Use as {@code durationMinutes} for a permanent (30-day) action. */
    public static final int MAX_DURATION_MINUTES = 60 * 24 * 30; // 30 days

    static final String BAN_KEY  = "moderation:ban:%s";
    static final String MUTE_KEY = "moderation:mute:%s";
    static final Duration MAX_DURATION = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;
    private final ChatMessageRepository chatMessageRepository;

    // ------------------------------------------------------------------ BAN

    /**
     * Bans a user for {@code durationMinutes} minutes.
     * Banned users' STOMP messages will be rejected by the channel interceptor.
     */
    public void banUser(UUID userId, int durationMinutes) {
        Duration ttl = resolvedDuration(durationMinutes);
        redisTemplate.opsForValue().set(banKey(userId), Instant.now().plus(ttl).toString(), ttl);
        log.info("User {} banned for {} minutes", userId, ttl.toMinutes());
    }

    /**
     * Removes an active ban for {@code userId}.
     */
    public void unbanUser(UUID userId) {
        redisTemplate.delete(banKey(userId));
        log.info("User {} unbanned", userId);
    }

    /** Returns {@code true} if the user is currently banned. */
    public boolean isBanned(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(banKey(userId)));
    }

    // ----------------------------------------------------------------- MUTE

    /**
     * Mutes a user for {@code durationMinutes} minutes.
     * Muted users cannot send messages but remain connected to receive them.
     */
    public void muteUser(UUID userId, int durationMinutes) {
        Duration ttl = resolvedDuration(durationMinutes);
        redisTemplate.opsForValue().set(muteKey(userId), Instant.now().plus(ttl).toString(), ttl);
        log.info("User {} muted for {} minutes", userId, ttl.toMinutes());
    }

    /**
     * Removes an active mute for {@code userId}.
     */
    public void unmuteUser(UUID userId) {
        redisTemplate.delete(muteKey(userId));
        log.info("User {} unmuted", userId);
    }

    /** Returns {@code true} if the user is currently muted. */
    public boolean isMuted(UUID userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(muteKey(userId)));
    }

    // ------------------------------------------------------- DELETE MESSAGE

    /**
     * Soft-deletes a chat message by setting its {@code deleted} flag.
     * The message record is retained in the database for audit purposes.
     *
     * @throws MessageNotFoundException if the message does not exist
     */
    @Transactional
    public void deleteMessage(UUID messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException(
                        "Message not found: " + messageId));
        message.setDeleted(true);
        chatMessageRepository.save(message);
        log.info("Message {} soft-deleted by moderator", messageId);
    }

    // ---------------------------------------------------------------- helpers

    private static Duration resolvedDuration(int durationMinutes) {
        if (durationMinutes < 0) {
            throw new IllegalArgumentException("durationMinutes must be >= 0");
        }
        return durationMinutes == 0
                ? MAX_DURATION
                : Duration.ofMinutes(Math.min(durationMinutes, MAX_DURATION_MINUTES));
    }

    static String banKey(UUID userId)  { return String.format(BAN_KEY,  userId); }
    static String muteKey(UUID userId) { return String.format(MUTE_KEY, userId); }
}
