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
import pse.trippy.userservice.dto.request.RegisterRequest;
import pse.trippy.userservice.dto.response.RegisterResponse;
import pse.trippy.userservice.exception.EmailAlreadyExistsException;
import pse.trippy.userservice.service.AuthService;
import pse.trippy.userservice.TestFixtures;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link AuthController}.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private static final String REGISTER_URL = "/auth/register";

    // =========================================================================
    // POST /auth/register
    // =========================================================================

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("returns 201 Created for valid registration")
        void returnsCreatedForValidRequest() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            RegisterRequest request = RegisterRequest.builder()
                    .email("john@example.com")
                    .password(TestFixtures.validPassword())
                    .displayName("John Doe")
                    .build();

            RegisterResponse response = RegisterResponse.builder()
                    .userId(userId)
                    .email("john@example.com")
                    .message("Registration successful. You can sign in now.")
                    .verificationRequired(false)
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            // When/Then
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.verificationRequired").value(false));
        }

        @Test
        @DisplayName("returns 409 Conflict for duplicate email")
        void returnsConflictForDuplicateEmail() throws Exception {
            // Given
            RegisterRequest request = RegisterRequest.builder()
                    .email("existing@example.com")
                    .password(TestFixtures.validPassword())
                    .displayName("John Doe")
                    .build();

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new EmailAlreadyExistsException("existing@example.com"));

            // When/Then
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("returns 400 Bad Request for invalid email format")
        void returnsBadRequestForInvalidEmail() throws Exception {
            // Given
            RegisterRequest request = RegisterRequest.builder()
                    .email("invalid-email")
                    .password(TestFixtures.validPassword())
                    .displayName("John Doe")
                    .build();

            // When/Then
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details[?(@.field=='email')]").exists());
        }

        @Test
        @DisplayName("returns 400 Bad Request for weak password")
        void returnsBadRequestForWeakPassword() throws Exception {
            // Given - password missing special char
            RegisterRequest request = RegisterRequest.builder()
                    .email("john@example.com")
                    .password(TestFixtures.weakPassword())
                    .displayName("John Doe")
                    .build();

            // When/Then
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details[?(@.field=='password')]").exists());
        }

        @Test
        @DisplayName("returns 400 Bad Request for missing displayName")
        void returnsBadRequestForMissingDisplayName() throws Exception {
            // Given
            String requestJson = String.format("""
                    {
                        "email": "john@example.com",
                        "password": "%s"
                    }
                    """, TestFixtures.validPassword());

            // When/Then
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details[?(@.field=='displayName')]").exists());
        }

        @Test
        @DisplayName("returns 400 Bad Request for short password")
        void returnsBadRequestForShortPassword() throws Exception {
            // Given
            RegisterRequest request = RegisterRequest.builder()
                    .email("john@example.com")
                    .password(TestFixtures.shortPassword())
                    .displayName("John Doe")
                    .build();

            // When/Then
            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }
}