package pse.trippy.chatservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pse.trippy.chatservice.config.GatewayHeaderAuthFilter;
import pse.trippy.chatservice.config.SecurityConfig;
import pse.trippy.chatservice.service.ModerationService;

import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModerationController.class)
@Import({SecurityConfig.class, GatewayHeaderAuthFilter.class})
@DisplayName("ModerationController")
class ModerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModerationService moderationService;

    // -------------------------------------------------------------------- ban

    @Test
    @DisplayName("POST /admin/chat/users/{userId}/ban returns 204 for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void banUser_admin_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(moderationService).banUser(userId, 60);

        mockMvc.perform(post("/admin/chat/users/{userId}/ban", userId)
                        .param("durationMinutes", "60")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(moderationService).banUser(userId, 60);
    }

    @Test
    @DisplayName("POST /admin/chat/users/{userId}/ban returns 403 for non-ADMIN")
    @WithMockUser(roles = "USER")
    void banUser_nonAdmin_returns403() throws Exception {
        mockMvc.perform(post("/admin/chat/users/{userId}/ban", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /admin/chat/users/{userId}/ban returns 403 for anonymous (no auth)")
    void banUser_anonymous_returns403() throws Exception {
        mockMvc.perform(post("/admin/chat/users/{userId}/ban", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /admin/chat/users/{userId}/ban returns 204 for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void unbanUser_admin_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(moderationService).unbanUser(userId);

        mockMvc.perform(delete("/admin/chat/users/{userId}/ban", userId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(moderationService).unbanUser(userId);
    }

    // ------------------------------------------------------------------- mute

    @Test
    @DisplayName("POST /admin/chat/users/{userId}/mute returns 204 for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void muteUser_admin_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(moderationService).muteUser(userId, 30);

        mockMvc.perform(post("/admin/chat/users/{userId}/mute", userId)
                        .param("durationMinutes", "30")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(moderationService).muteUser(userId, 30);
    }

    @Test
    @DisplayName("DELETE /admin/chat/users/{userId}/mute returns 204 for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void unmuteUser_admin_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(moderationService).unmuteUser(userId);

        mockMvc.perform(delete("/admin/chat/users/{userId}/mute", userId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(moderationService).unmuteUser(userId);
    }

    // ---------------------------------------------------------- deleteMessage

    @Test
    @DisplayName("DELETE /admin/chat/messages/{messageId} returns 204 for ADMIN")
    @WithMockUser(roles = "ADMIN")
    void deleteMessage_admin_returns204() throws Exception {
        UUID messageId = UUID.randomUUID();
        doNothing().when(moderationService).deleteMessage(messageId);

        mockMvc.perform(delete("/admin/chat/messages/{messageId}", messageId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(moderationService).deleteMessage(messageId);
    }

    @Test
    @DisplayName("DELETE /admin/chat/messages/{messageId} returns 403 for non-ADMIN")
    @WithMockUser(roles = "USER")
    void deleteMessage_nonAdmin_returns403() throws Exception {
        mockMvc.perform(delete("/admin/chat/messages/{messageId}", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
