package pse.trippy.chatservice.dto.request;

/**
 * STOMP payload for {@code /app/trips/{tripId}/typing}.
 *
 * @param typing {@code true} when the user starts typing, {@code false} when they stop.
 */
public record TypingRequest(boolean typing) {}
