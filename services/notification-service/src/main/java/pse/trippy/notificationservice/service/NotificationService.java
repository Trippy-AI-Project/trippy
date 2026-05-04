package pse.trippy.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.notificationservice.dto.response.NotificationResponse;
import pse.trippy.notificationservice.model.entity.Notification;
import pse.trippy.notificationservice.model.enums.NotificationChannel;
import pse.trippy.notificationservice.model.enums.NotificationType;
import pse.trippy.notificationservice.repository.NotificationRepository;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String APP_HOST = "trippy.app";

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification createNotification(UUID userId, NotificationType type, String title,
                                           String message, String actionUrl) {
        return createNotification(userId, type, title, message, actionUrl, Map.of());
    }

    @Transactional
    public Notification createNotification(UUID userId, NotificationType type, String title,
                                           String message, String actionUrl, Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .actionUrl(normalizeActionUrl(actionUrl))
                .metadata(metadata == null ? new HashMap<>() : new HashMap<>(metadata))
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created in-app notification [{}] for user {}", type, userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, int page, int size) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(
                        userId,
                        PageRequest.of(Math.max(page, 0), boundedPageSize(size)));
        return notifications.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalseAndDeletedFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId) && !n.isDeleted()) {
                n.setRead(true);
                if (n.getReadAt() == null) {
                    n.setReadAt(Instant.now());
                }
                notificationRepository.save(n);
                log.info("Marked notification {} as read for user {}", notificationId, userId);
            }
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndReadFalseAndDeletedFalseOrderByCreatedAtDesc(userId);
        Instant readAt = Instant.now();
        unread.forEach(n -> {
            n.setRead(true);
            n.setReadAt(readAt);
        });
        notificationRepository.saveAll(unread);
        log.info("Marked all notifications as read for user {}", userId);
    }

    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setDeleted(true);
                notificationRepository.save(n);
                log.info("Soft-deleted notification {} for user {}", notificationId, userId);
            }
        });
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getMessage(),
                n.getActionUrl(),
                n.isRead(),
                n.getReadAt(),
                n.getMetadata(),
                n.getCreatedAt()
        );
    }

    private int boundedPageSize(int requestedSize) {
        if (requestedSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(requestedSize, MAX_PAGE_SIZE);
    }

    private String normalizeActionUrl(String actionUrl) {
        if (actionUrl == null || actionUrl.isBlank()) {
            return null;
        }

        String trimmed = actionUrl.trim();
        try {
            URI uri = new URI(trimmed);
            if (uri.isAbsolute()) {
                if (!"https".equalsIgnoreCase(uri.getScheme())
                        || !APP_HOST.equalsIgnoreCase(uri.getHost())) {
                    log.warn("Dropping unsafe notification action URL host: {}", trimmed);
                    return null;
                }
                return normalizeInternalPath(uri);
            }

            if (trimmed.startsWith("//")) {
                log.warn("Dropping protocol-relative notification action URL: {}", trimmed);
                return null;
            }
            return normalizeInternalPath(uri);
        } catch (URISyntaxException ex) {
            log.warn("Dropping invalid notification action URL: {}", trimmed);
            return null;
        }
    }

    private String normalizeInternalPath(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isBlank() || !path.startsWith("/") || containsParentTraversal(path)) {
            log.warn("Dropping unsafe notification action path: {}", path);
            return null;
        }

        StringBuilder normalized = new StringBuilder(path);
        if (uri.getRawQuery() != null) {
            normalized.append('?').append(uri.getRawQuery());
        }
        if (uri.getRawFragment() != null) {
            normalized.append('#').append(uri.getRawFragment());
        }
        return normalized.toString();
    }

    private boolean containsParentTraversal(String path) {
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        return Arrays.asList(decodedPath.split("/")).contains("..");
    }
}
