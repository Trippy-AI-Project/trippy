package pse.trippy.notificationservice.dto.response;

import pse.trippy.notificationservice.model.enums.NotificationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID notificationId,
        UUID userId,
        NotificationType type,
        String title,
        String message,
        String body,
        String actionUrl,
        Map<String, Object> metadata,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
}
