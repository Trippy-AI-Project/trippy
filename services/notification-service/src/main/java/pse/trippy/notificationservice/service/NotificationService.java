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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification createNotification(UUID userId, NotificationType type, String title,
                                           String message, String actionUrl) {
        return createNotification(userId, type, title, message, actionUrl, null);
    }

    @Transactional
    public Notification createNotification(UUID userId, NotificationType type, String title,
                                           String message, String actionUrl,
                                           Map<String, Object> metadata) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .metadata(metadata)
                .channel(NotificationChannel.IN_APP)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Created in-app notification [{}] for user {}", type, userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, int page, int size) {
        return getNotifications(userId, page, size, false, null);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, int page, int size,
                                                       boolean unreadOnly, NotificationType type) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(page, 0),
                Math.max(1, Math.min(size, 50)));

        Page<Notification> notifications;
        if (type != null && unreadOnly) {
            notifications = notificationRepository
                    .findByUserIdAndTypeAndReadFalseAndDeletedFalseOrderByCreatedAtDesc(
                            userId, type, pageRequest);
        } else if (type != null) {
            notifications = notificationRepository
                    .findByUserIdAndTypeAndDeletedFalseOrderByCreatedAtDesc(userId, type, pageRequest);
        } else if (unreadOnly) {
            notifications = notificationRepository
                    .findByUserIdAndReadFalseAndDeletedFalseOrderByCreatedAtDesc(userId, pageRequest);
        } else {
            notifications = notificationRepository
                    .findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageRequest);
        }

        return notifications.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalseAndDeletedFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            if (n.getReadAt() == null) {
                n.setReadAt(Instant.now());
            }
            notificationRepository.save(n);
            log.info("Marked notification {} as read", notificationId);
        });
    }

    @Transactional
    public boolean markAsRead(UUID notificationId, UUID userId) {
        return notificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, userId)
                .map(n -> {
                    n.setRead(true);
                    if (n.getReadAt() == null) {
                        n.setReadAt(Instant.now());
                    }
                    notificationRepository.save(n);
                    log.info("Marked notification {} as read for user {}", notificationId, userId);
                    return true;
                })
                .orElse(false);
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
    public boolean deleteNotification(UUID notificationId, UUID userId) {
        return notificationRepository.findByIdAndUserIdAndDeletedFalse(notificationId, userId)
                .map(n -> {
                    n.setDeleted(true);
                    notificationRepository.save(n);
                    log.info("Soft-deleted notification {} for user {}", notificationId, userId);
                    return true;
                })
                .orElse(false);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getId(),
                n.getUserId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getMessage(),
                n.getActionUrl(),
                n.getMetadata(),
                n.isRead(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
