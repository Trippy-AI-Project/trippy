package pse.trippy.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pse.trippy.userservice.config.SecurityConfig;
import pse.trippy.userservice.dto.request.UpdateProfileRequest;
import pse.trippy.userservice.dto.response.UserProfileResponse;
import pse.trippy.userservice.exception.UserNotFoundException;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;
import pse.trippy.userservice.service.EmailVerificationService;
import pse.trippy.userservice.service.UserProfileService;
import pse.trippy.userservice.service.UserService;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link UserController}.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private EmailVerificationService emailVerificationService;

    @MockBean 
    private UserService userService;

    private static final String GET_ME_URL = "/users/me";
    private static final String PATCH_ME_URL = "/users/me";

    private UserProfileResponse sampleProfile(UUID id) {
        return UserProfileResponse.builder()
                .id(id)
                .email("user@example.com")
                .displayName("John Doe")
                .bio("Travel enthusiast")
                .phoneNumber("+49123456789")
                .avatarUrl(null)
                .role(UserRole.USER)
                .plan(SubscriptionPlan.FREE)
                .emailVerified(false)
                .createdAt(Instant.parse("2026-03-28T09:00:00Z"))
                .build();
    }

    // =========================================================================
    // GET /users/me
    // =========================================================================

    @Nested
    @DisplayName("GET /users/me")
    class GetProfile {

        @Test
        @DisplayName("returns 200 with profile when X-User-Id present")
        void returns200WithProfile() throws Exception {
            UUID userId = UUID.randomUUID();
            when(userProfileService.getProfile(userId)).thenReturn(sampleProfile(userId));

            mockMvc.perform(get(GET_ME_URL)
                            .header("X-User-Id", userId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId.toString()))
                    .andExpect(jsonPath("$.email").value("user@example.com"))
                    .andExpect(jsonPath("$.displayName").value("John Doe"))
                    .andExpect(jsonPath("$.role").value("USER"))
                    .andExpect(jsonPath("$.plan").value("FREE"))
                    .andExpect(jsonPath("$.emailVerified").value(false));
        }

        @Test
        @DisplayName("returns 401 when X-User-Id header is missing")
        void returns401WhenHeaderMissing() throws Exception {
            mockMvc.perform(get(GET_ME_URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 404 when user not found")
        void returns404WhenUserNotFound() throws Exception {
            UUID userId = UUID.randomUUID();
            when(userProfileService.getProfile(userId))
                    .thenThrow(new UserNotFoundException(userId));

            mockMvc.perform(get(GET_ME_URL)
                            .header("X-User-Id", userId.toString()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("USER_NOT_FOUND"));
        }
    }

    // =========================================================================
    // PATCH /users/me
    // =========================================================================

    @Nested
    @DisplayName("PATCH /users/me")
    class UpdateProfile {

        @Test
        @DisplayName("returns 200 with updated profile when X-User-Id present")
        void returns200WithUpdatedProfile() throws Exception {
            UUID userId = UUID.randomUUID();
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .displayName("Jane Doe")
                    .bio("Updated bio")
                    .build();

            UserProfileResponse updated = sampleProfile(userId);
            updated.setDisplayName("Jane Doe");
            updated.setBio("Updated bio");

            when(userProfileService.updateProfile(eq(userId), any(UpdateProfileRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(patch(PATCH_ME_URL)
                            .header("X-User-Id", userId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.displayName").value("Jane Doe"))
                    .andExpect(jsonPath("$.bio").value("Updated bio"));
        }

        @Test
        @DisplayName("returns 401 when X-User-Id header is missing")
        void returns401WhenHeaderMissing() throws Exception {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .displayName("Jane Doe")
                    .build();

            mockMvc.perform(patch(PATCH_ME_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 400 when displayName is blank")
        void returns400WhenDisplayNameIsBlank() throws Exception {
            UUID userId = UUID.randomUUID();
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .displayName("")
                    .build();

            mockMvc.perform(patch(PATCH_ME_URL)
                            .header("X-User-Id", userId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("returns 400 when bio exceeds 500 characters")
        void returns400WhenBioTooLong() throws Exception {
            UUID userId = UUID.randomUUID();
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .bio("x".repeat(501))
                    .build();

            mockMvc.perform(patch(PATCH_ME_URL)
                            .header("X-User-Id", userId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }
}
