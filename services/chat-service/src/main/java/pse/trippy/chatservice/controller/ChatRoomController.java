package pse.trippy.chatservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.chatservice.dto.response.ChatRoomResponse;
import pse.trippy.chatservice.service.ChatRoomService;

import java.util.UUID;

/**
 * REST controller for chat room management.
 * Called internally by Trip Service when a trip is created.
 */
@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/trips/{tripId}/chat/rooms")
    public ResponseEntity<ChatRoomResponse> createRoom(@PathVariable UUID tripId) {
        ChatRoomResponse response = chatRoomService.createRoom(tripId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
