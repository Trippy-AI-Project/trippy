package pse.trippy.chatservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.chatservice.service.ModerationService;

import java.util.UUID;

/**
 * REST API for ticket 4.4 — Chat Moderation.
 *
 * <p>All endpoints require the caller to have the {@code ADMIN} role, which is
 * enforced both at the HTTP security layer (via
 * {@link pse.trippy.chatservice.config.SecurityConfig}) and at the method level
 * via {@link PreAuthorize}. In production the API gateway only routes requests
 * with a validated ADMIN JWT to these paths.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST /admin/chat/users/{userId}/ban[?durationMinutes=N]}  — ban user</li>
 *   <li>{@code DELETE /admin/chat/users/{userId}/ban}                    — remove ban</li>
 *   <li>{@code POST /admin/chat/users/{userId}/mute[?durationMinutes=N]} — mute user</li>
 *   <li>{@code DELETE /admin/chat/users/{userId}/mute}                   — remove mute</li>
 *   <li>{@code DELETE /admin/chat/messages/{messageId}}                  — soft-delete message</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/chat")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ModerationController {

    private final ModerationService moderationService;

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<Void> banUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int durationMinutes) {
        moderationService.banUser(userId, durationMinutes);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}/ban")
    public ResponseEntity<Void> unbanUser(@PathVariable UUID userId) {
        moderationService.unbanUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/mute")
    public ResponseEntity<Void> muteUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int durationMinutes) {
        moderationService.muteUser(userId, durationMinutes);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}/mute")
    public ResponseEntity<Void> unmuteUser(@PathVariable UUID userId) {
        moderationService.unmuteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageId) {
        moderationService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }
}
