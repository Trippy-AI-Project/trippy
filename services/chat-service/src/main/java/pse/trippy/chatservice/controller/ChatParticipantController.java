package pse.trippy.chatservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.chatservice.service.ChatPresenceService;

import java.util.Set;
import java.util.UUID;

/**
 * REST endpoint for querying currently connected chat participants.
 */
@RestController
@RequiredArgsConstructor
public class ChatParticipantController {

    private final ChatPresenceService chatPresenceService;

    @GetMapping("/chats/{tripId}/participants")
    public ResponseEntity<Set<UUID>> getParticipants(@PathVariable UUID tripId) {
        return ResponseEntity.ok(chatPresenceService.getConnectedUsers(tripId));
    }
}
