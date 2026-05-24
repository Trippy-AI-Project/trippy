package pse.trippy.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pse.trippy.userservice.dto.request.RegisterRequest;
import pse.trippy.userservice.dto.response.RegisterResponse;
import pse.trippy.userservice.exception.EmailAlreadyExistsException;
import pse.trippy.userservice.repository.UserRepository;
import pse.trippy.userservice.service.AuthService;
import pse.trippy.userservice.service.JwtService;
import pse.trippy.userservice.service.UserService;
import pse.trippy.userservice.TestFixtures;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;

/**
 * Integration tests for {@link AuthController}.
 */

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class,
                DataSourceAutoConfiguration.class,
                HibernateJpaAutoConfiguration.class,
                JpaRepositoriesAutoConfiguration.class,
                JdbcTemplateAutoConfiguration.class
        }
)
@TestPropertySource(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private UserRepository userRepository;

    private static final String REGISTER_URL = "/auth/register";

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterEndpoint {

        @Test
        @DisplayName("returns 201 Created for valid registration")
        void returnsCreatedForValidRequest() throws Exception {
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
            RegisterRequest request = RegisterRequest.builder()
                    .email("existing@example.com")
                    .password(TestFixtures.validPassword())
                    .displayName("John Doe")
                    .build();

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new EmailAlreadyExistsException("existing@example.com"));

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("returns 400 Bad Request for invalid email")
        void returnsBadRequestForInvalidEmail() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("invalid-email")
                    .password(TestFixtures.validPassword())
                    .displayName("John Doe")
                    .build();

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
            RegisterRequest request = RegisterRequest.builder()
                    .email("john@example.com")
                    .password(TestFixtures.weakPassword())
                    .displayName("John Doe")
                    .build();

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
            String requestJson = String.format("""
                    {
                        "email": "john@example.com",
                        "password": "%s"
                    }
                    """, TestFixtures.validPassword());

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
            RegisterRequest request = RegisterRequest.builder()
                    .email("john@example.com")
                    .password(TestFixtures.shortPassword())
                    .displayName("John Doe")
                    .build();

            mockMvc.perform(post(REGISTER_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
    }
}