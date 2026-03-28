package pse.trippy.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pse.trippy.userservice.dto.request.RegisterRequest;
import pse.trippy.userservice.dto.response.RegisterResponse;
import pse.trippy.userservice.exception.EmailAlreadyExistsException;
import pse.trippy.userservice.model.entity.User;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;
import pse.trippy.userservice.repository.UserRepository;
import pse.trippy.userservice.TestFixtures;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;
    private static final String TEST_EMAIL = "john@example.com";
    private static final String TEST_PASSWORD = TestFixtures.validPassword();
    private static final String TEST_DISPLAY_NAME = "John Doe";
    private static final String HASHED_PASSWORD = TestFixtures.bcryptHash();

    @BeforeEach
    void setUp() {
        validRequest = RegisterRequest.builder()
                .email(TEST_EMAIL)
                .password(TEST_PASSWORD)
                .displayName(TEST_DISPLAY_NAME)
                .build();
    }

    // =========================================================================
    // register
    // =========================================================================

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("creates user successfully with correct data")
        void createsUserSuccessfully() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(userId);
                return user;
            });

            // When
            RegisterResponse response = authService.register(validRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.getMessage()).contains("Registration successful");
            assertThat(response.isVerificationRequired()).isTrue();

            // Verify user saved with correct values
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(savedUser.getPasswordHash()).isEqualTo(HASHED_PASSWORD);
            assertThat(savedUser.getDisplayName()).isEqualTo(TEST_DISPLAY_NAME);
            assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
            assertThat(savedUser.getPlan()).isEqualTo(SubscriptionPlan.FREE);
            assertThat(savedUser.isEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("hashes password with BCrypt, never stores plaintext")
        void hashesPasswordWithBCrypt() {
            // Given
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });

            // When
            authService.register(validRequest);

            // Then
            verify(passwordEncoder).encode(TEST_PASSWORD);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            // Password should be hashed, not plaintext
            assertThat(userCaptor.getValue().getPasswordHash())
                    .isEqualTo(HASHED_PASSWORD)
                    .isNotEqualTo(TEST_PASSWORD);
        }

        @Test
        @DisplayName("throws EmailAlreadyExistsException when email is duplicate")
        void throwsExceptionForDuplicateEmail() {
            // Given
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.register(validRequest))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining(TEST_EMAIL);

            // Verify no user saved
            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        @DisplayName("returns userId and email, never password")
        void responseNeverContainsPassword() {
            // Given
            UUID userId = UUID.randomUUID();
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(userId);
                return user;
            });

            // When
            RegisterResponse response = authService.register(validRequest);

            // Then - response should only contain userId and email, no password fields
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            // RegisterResponse class has no password field by design
        }
    }
}
