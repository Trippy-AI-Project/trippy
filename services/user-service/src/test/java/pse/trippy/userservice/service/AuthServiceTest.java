package pse.trippy.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pse.trippy.userservice.TestFixtures;
import pse.trippy.userservice.dto.request.RegisterRequest;
import pse.trippy.userservice.dto.response.RegisterResponse;
import pse.trippy.userservice.exception.AccountNotVerifiedException;
import pse.trippy.userservice.exception.EmailAlreadyExistsException;
import pse.trippy.userservice.exception.InvalidCredentialsException;
import pse.trippy.userservice.exception.InvalidTokenException;
import pse.trippy.userservice.model.dto.LoginRequest;
import pse.trippy.userservice.model.dto.LoginResponse;
import pse.trippy.userservice.model.dto.RefreshTokenRequest;
import pse.trippy.userservice.model.dto.TokenResponse;
import pse.trippy.userservice.model.entity.RefreshToken;
import pse.trippy.userservice.model.entity.User;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;
import pse.trippy.userservice.repository.RefreshTokenRepository;
import pse.trippy.userservice.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}.
 *
 * <p>All dependencies are mocked — no Spring context or database required.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private static final int REFRESH_EXPIRY_DAYS = 7;
    private static final int REMEMBER_ME_EXPIRY_DAYS = 30;
    private static final String ACCESS_TOKEN = "mock-access-token";
    private static final int ACCESS_TOKEN_EXPIRY = 900;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                jwtService,
                REFRESH_EXPIRY_DAYS,
                REMEMBER_ME_EXPIRY_DAYS
        );
    }

    private User buildUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("bob@example.com")
                .passwordHash("mock-pw-hash")
                .displayName("Bob")
                .role(UserRole.USER)
                .plan(SubscriptionPlan.FREE)
                .emailVerified(true)
                .build();
    }

    // =========================================================================
    // register
    // =========================================================================

    @Nested
    @DisplayName("register")
    class Register {

        private final String TEST_EMAIL = "john@example.com";
        private final String TEST_PASSWORD = "SecureP@ssword123";
        private final String HASHED_PASSWORD = "mock-hashed-password";
        private final String TEST_DISPLAY_NAME = "John Doe";

        @Test
        @DisplayName("creates user successfully with correct data")
        void createsUserSuccessfully() {
            RegisterRequest request = RegisterRequest.builder()
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .displayName(TEST_DISPLAY_NAME)
                    .build();

            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RegisterResponse response = authService.register(request);

            assertThat(response).isNotNull();
            assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(response.isVerificationRequired()).isFalse();

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("throws EmailAlreadyExistsException when email is taken")
        void throwsWhenEmailTaken() {
            RegisterRequest request = RegisterRequest.builder()
                    .email(TEST_EMAIL)
                    .password(TEST_PASSWORD)
                    .displayName(TEST_DISPLAY_NAME)
                    .build();

            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class);
        }
    }

    // =========================================================================
    // login
    // =========================================================================

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns tokens and user profile when credentials are valid")
        void returnsTokensForValidCredentials() {
            User user = buildUser();
            LoginRequest request = LoginRequest.builder()
                    .email("bob@example.com")
                    .password("mock-password")
                    .build();

            when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("mock-password", "mock-pw-hash")).thenReturn(true);
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(ACCESS_TOKEN_EXPIRY);

            LoginResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("bob@example.com");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("throws AccountNotVerifiedException when email is not verified")
        void throwsWhenEmailNotVerified() {
            User user = buildUser();
            user.setEmailVerified(false);

            LoginRequest request = LoginRequest.builder()
                    .email("bob@example.com")
                    .password("mock-password")
                    .build();

            when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("mock-password", "mock-pw-hash")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AccountNotVerifiedException.class);
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when password is wrong")
        void throwsWhenPasswordIsWrong() {
            User user = buildUser();
            LoginRequest request = LoginRequest.builder()
                    .email("bob@example.com")
                    .password("wrong-password")
                    .build();

            when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong-password", "mock-pw-hash")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    // =========================================================================
    // refreshToken
    // =========================================================================

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("rotates token and returns new pair when refresh token is valid")
        void rotatesTokenForValidRefreshToken() {
            User user = buildUser();
            String rawToken = UUID.randomUUID().toString();
            String hashedToken = AuthService.hashToken(rawToken);

            RefreshToken storedToken = RefreshToken.builder()
                    .token(hashedToken)
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .build();

            when(refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(storedToken));
            when(refreshTokenRepository.deleteByTokenValue(hashedToken)).thenReturn(1);
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(ACCESS_TOKEN_EXPIRY);

            TokenResponse response = authService.refreshToken(new RefreshTokenRequest(rawToken));

            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            verify(refreshTokenRepository).deleteByTokenValue(hashedToken);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }
    }
}
