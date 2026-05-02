package pse.trippy.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pse.trippy.notificationservice.dto.response.NotificationResponse;
import pse.trippy.notificationservice.model.enums.NotificationType;
import pse.trippy.notificationservice.service.NotificationService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) NotificationType type) {
        return ResponseEntity.ok(notificationService.getNotifications(userId, page, size, unreadOnly, type));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("X-User-Id") UUID userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count, "unreadCount", count));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        if (!notificationService.markAsRead(id, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsReadPost(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        if (!notificationService.markAsRead(id, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("X-User-Id") UUID userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsReadPost(
            @RequestHeader("X-User-Id") UUID userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        if (!notificationService.deleteNotification(id, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
        return ResponseEntity.noContent().build();
    }
}
