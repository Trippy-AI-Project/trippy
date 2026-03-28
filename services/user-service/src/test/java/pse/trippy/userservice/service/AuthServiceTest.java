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
                .emailVerified(false)
                .build();
    }

    // =========================================================================
    // login
    // =========================================================================

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns tokens when credentials are valid")
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
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getExpiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRY);
            assertThat(response.getTokenType()).isEqualTo("Bearer");

            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("persists refresh token with SHA-256 hash, not raw value")
        void persistsHashedRefreshToken() {
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

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(captor.capture());
            RefreshToken saved = captor.getValue();

            // The stored hash must differ from the raw token returned to the client
            assertThat(saved.getToken()).isNotEqualTo(response.getRefreshToken());
            // The stored hash must equal SHA-256(raw token)
            assertThat(saved.getToken()).isEqualTo(AuthService.hashToken(response.getRefreshToken()));
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when user is not found")
        void throwsWhenUserNotFound() {
            LoginRequest request = LoginRequest.builder()
                    .email("unknown@example.com")
                    .password("whatever")
                    .build();

            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");

            verify(refreshTokenRepository, never()).save(any());
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
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");

            verify(refreshTokenRepository, never()).save(any());
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
                    .id(UUID.randomUUID())
                    .token(hashedToken)
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .build();

            when(refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(storedToken));
            when(jwtService.generateAccessToken(user)).thenReturn(ACCESS_TOKEN);
            when(jwtService.getAccessTokenExpirySeconds()).thenReturn(ACCESS_TOKEN_EXPIRY);

            TokenResponse response = authService.refreshToken(new RefreshTokenRequest(rawToken));

            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isNotBlank();
            assertThat(response.getRefreshToken()).isNotEqualTo(rawToken);
            assertThat(response.getExpiresIn()).isEqualTo(ACCESS_TOKEN_EXPIRY);

            // Old token deleted, new one saved
            verify(refreshTokenRepository).delete(storedToken);
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("throws InvalidTokenException when refresh token is not found")
        void throwsWhenRefreshTokenNotFound() {
            String rawToken = UUID.randomUUID().toString();
            String hashedToken = AuthService.hashToken(rawToken);

            when(refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(rawToken)))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("throws InvalidTokenException and deletes token when it is expired")
        void throwsAndDeletesWhenTokenExpired() {
            User user = buildUser();
            String rawToken = UUID.randomUUID().toString();
            String hashedToken = AuthService.hashToken(rawToken);

            RefreshToken expiredToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token(hashedToken)
                    .user(user)
                    .expiresAt(Instant.now().minusSeconds(60))
                    .build();

            when(refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(expiredToken));

            assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(rawToken)))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessage("Refresh token has expired");

            verify(refreshTokenRepository).delete(expiredToken);
        }
    }

    // =========================================================================
    // hashToken
    // =========================================================================

    @Nested
    @DisplayName("hashToken")
    class HashToken {

        @Test
        @DisplayName("produces a deterministic hash for the same input")
        void isDeterministic() {
            String input = "test-token-value";
            assertThat(AuthService.hashToken(input)).isEqualTo(AuthService.hashToken(input));
        }

        @Test
        @DisplayName("produces different hashes for different inputs")
        void differentInputsProduceDifferentHashes() {
            assertThat(AuthService.hashToken("token-a")).isNotEqualTo(AuthService.hashToken("token-b"));
        }
    }
}
