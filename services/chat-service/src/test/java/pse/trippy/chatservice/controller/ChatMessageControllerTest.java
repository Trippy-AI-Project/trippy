package pse.trippy.chatservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pse.trippy.chatservice.dto.request.SendMessageRequest;
import pse.trippy.chatservice.dto.response.ChatMessageResponse;
import pse.trippy.chatservice.model.enums.MessageType;
import pse.trippy.chatservice.service.ChatMessageService;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatMessageController.class)
@DisplayName("ChatMessageController")
class ChatMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatMessageService chatMessageService;

    @Test
    @DisplayName("POST /trips/{tripId}/chat/messages returns 201")
    void sendMessageReturnsCreated() throws Exception {
        UUID tripId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();

        when(chatMessageService.sendMessage(eq(tripId), any(), anyString(), anyString(), any()))
                .thenReturn(ChatMessageResponse.builder()
                        .id(msgId)
                        .senderId(UUID.randomUUID())
                        .senderDisplayName("Alice")
                        .content("Hello!")
                        .type("TEXT")
                        .createdAt(Instant.now())
                        .edited(false)
                        .build());

        SendMessageRequest request = SendMessageRequest.builder()
                .content("Hello!")
                .type("TEXT")
                .build();

        mockMvc.perform(post("/trips/{tripId}/chat/messages", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(msgId.toString()))
                .andExpect(jsonPath("$.content").value("Hello!"))
                .andExpect(jsonPath("$.type").value("TEXT"));
    }

    @Test
    @DisplayName("POST returns 400 for empty content")
    void returnsBadRequestForEmptyContent() throws Exception {
        UUID tripId = UUID.randomUUID();

        String json = "{\"content\": \"\", \"type\": \"TEXT\"}";

        mockMvc.perform(post("/trips/{tripId}/chat/messages", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
