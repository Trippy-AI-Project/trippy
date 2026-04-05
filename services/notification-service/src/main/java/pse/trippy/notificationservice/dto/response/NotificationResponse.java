package pse.trippy.notificationservice.dto.response;

import pse.trippy.notificationservice.model.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationType type,
        String title,
        String message,
        String actionUrl,
        boolean read,
        Instant createdAt
) {
}
