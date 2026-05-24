package pse.trippy.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pse.trippy.userservice.dto.request.UpdateProfileRequest;
import pse.trippy.userservice.dto.response.UserProfileResponse;
import pse.trippy.userservice.exception.UserNotFoundException;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;
import pse.trippy.userservice.repository.UserRepository;
import pse.trippy.userservice.service.AuthService;
import pse.trippy.userservice.service.EmailVerificationService;
import pse.trippy.userservice.service.JwtService;
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
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc(addFilters = false)
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
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

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
                    .andExpect(jsonPath("$.id").value(userId.toString()));
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
                    .andExpect(status().isNotFound());
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

            when(userProfileService.updateProfile(eq(userId), any()))
                    .thenReturn(updated);

            mockMvc.perform(patch(PATCH_ME_URL)
                            .header("X-User-Id", userId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 401 when X-User-Id header is missing")
        void returns401WhenHeaderMissing() throws Exception {
            mockMvc.perform(patch(PATCH_ME_URL))
                    .andExpect(status().isUnauthorized());
        }
    }
}
