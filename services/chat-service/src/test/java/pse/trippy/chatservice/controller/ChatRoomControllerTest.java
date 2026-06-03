package pse.trippy.chatservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import pse.trippy.chatservice.dto.response.ChatRoomResponse;
import pse.trippy.chatservice.exception.ChatRoomAlreadyExistsException;
import pse.trippy.chatservice.service.ChatRoomService;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ChatRoomController")
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatRoomService chatRoomService;

    @Test
    @DisplayName("POST /trips/{tripId}/chat/rooms returns 201")
    void createRoomReturnsCreated() throws Exception {
        UUID tripId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();

        when(chatRoomService.createRoom(tripId)).thenReturn(ChatRoomResponse.builder()
                .id(roomId)
                .tripId(tripId)
                .createdAt(Instant.now())
                .build());

        mockMvc.perform(post("/trips/{tripId}/chat/rooms", tripId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(roomId.toString()))
                .andExpect(jsonPath("$.tripId").value(tripId.toString()));
    }

    @Test
    @DisplayName("POST /trips/{tripId}/chat/rooms returns 409 for duplicate")
    void createRoomReturnsConflict() throws Exception {
        UUID tripId = UUID.randomUUID();

        when(chatRoomService.createRoom(tripId))
                .thenThrow(new ChatRoomAlreadyExistsException(tripId.toString()));

        mockMvc.perform(post("/trips/{tripId}/chat/rooms", tripId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CHAT_ROOM_ALREADY_EXISTS"));
    }
}
