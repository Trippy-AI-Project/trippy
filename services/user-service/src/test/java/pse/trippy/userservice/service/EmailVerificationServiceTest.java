package pse.trippy.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import pse.trippy.userservice.exception.EmailAlreadyVerifiedException;
import pse.trippy.userservice.exception.InvalidTokenException;
import pse.trippy.userservice.exception.RateLimitExceededException;
import pse.trippy.userservice.exception.UserNotFoundException;
import pse.trippy.userservice.exception.VerificationTokenExpiredException;
import pse.trippy.userservice.model.entity.EmailVerificationToken;
import pse.trippy.userservice.model.entity.User;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;
import pse.trippy.userservice.repository.EmailVerificationTokenRepository;
import pse.trippy.userservice.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailVerificationService}.
 *
 * <p>All dependencies are mocked — no Spring context or database required.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService")
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(
                tokenRepository,
                userRepository,
                rabbitTemplate
        );
    }

    private User buildUser(boolean emailVerified) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .passwordHash("mock-pw-hash")
                .displayName("Alice")
                .role(UserRole.USER)
                .plan(SubscriptionPlan.FREE)
                .emailVerified(emailVerified)
                .build();
    }

    // =========================================================================
    // createVerificationToken
    // =========================================================================

    @Nested
    @DisplayName("createVerificationToken")
    class CreateVerificationToken {

        @Test
        @DisplayName("generates a UUID token and saves it with 24h expiry")
        void generatesTokenSuccessfully() {
            User user = buildUser(false);

            when(tokenRepository.save(any(EmailVerificationToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            String rawToken = emailVerificationService.createVerificationToken(user);

            assertThat(rawToken).isNotBlank();
            // UUID format check
            assertThat(UUID.fromString(rawToken)).isNotNull();

            ArgumentCaptor<EmailVerificationToken> captor =
                    ArgumentCaptor.forClass(EmailVerificationToken.class);
            verify(tokenRepository).save(captor.capture());

            EmailVerificationToken saved = captor.getValue();
            assertThat(saved.getToken()).isEqualTo(rawToken);
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(23, ChronoUnit.HOURS));
            assertThat(saved.getExpiresAt()).isBefore(Instant.now().plus(25, ChronoUnit.HOURS));
        }

        @Test
        @DisplayName("deletes existing tokens before creating a new one")
        void deletesExistingTokens() {
            User user = buildUser(false);

            when(tokenRepository.save(any(EmailVerificationToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            emailVerificationService.createVerificationToken(user);

            verify(tokenRepository).deleteAllByUserId(user.getId());
        }
    }

    // =========================================================================
    // verifyEmail
    // =========================================================================

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("sets emailVerified=true when token is valid")
        void verifiesEmailSuccessfully() {
            User user = buildUser(false);
            String rawToken = UUID.randomUUID().toString();
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .token(rawToken)
                    .user(user)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByToken(rawToken)).thenReturn(Optional.of(token));

            emailVerificationService.verifyEmail(rawToken);

            assertThat(user.isEmailVerified()).isTrue();
            verify(userRepository).save(user);
            verify(tokenRepository).deleteAllByUserId(user.getId());
        }

        @Test
        @DisplayName("publishes user.email.verified event on success")
        void publishesEventOnSuccess() {
            User user = buildUser(false);
            String rawToken = UUID.randomUUID().toString();
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .token(rawToken)
                    .user(user)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByToken(rawToken)).thenReturn(Optional.of(token));

            emailVerificationService.verifyEmail(rawToken);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);
            verify(rabbitTemplate).convertAndSend(
                    eq("user.events"),
                    eq("user.email.verified"),
                    eventCaptor.capture()
            );

            Map<String, Object> event = eventCaptor.getValue();
            assertThat(event.get("eventType")).isEqualTo("user.email.verified");
            assertThat(event.get("userId")).isEqualTo(user.getId().toString());
            assertThat(event.get("email")).isEqualTo(user.getEmail());
        }

        @Test
        @DisplayName("throws InvalidTokenException when token not found")
        void throwsWhenTokenNotFound() {
            when(tokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> emailVerificationService.verifyEmail("nonexistent"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid verification token");
        }

        @Test
        @DisplayName("throws VerificationTokenExpiredException when token is expired")
        void throwsWhenTokenExpired() {
            User user = buildUser(false);
            String rawToken = UUID.randomUUID().toString();
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .token(rawToken)
                    .user(user)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByToken(rawToken)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> emailVerificationService.verifyEmail(rawToken))
                    .isInstanceOf(VerificationTokenExpiredException.class)
                    .hasMessageContaining("expired");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws EmailAlreadyVerifiedException when user is already verified")
        void throwsWhenAlreadyVerified() {
            User user = buildUser(true);
            String rawToken = UUID.randomUUID().toString();
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .token(rawToken)
                    .user(user)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByToken(rawToken)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> emailVerificationService.verifyEmail(rawToken))
                    .isInstanceOf(EmailAlreadyVerifiedException.class)
                    .hasMessageContaining("already verified");
        }
    }

    // =========================================================================
    // resendVerification
    // =========================================================================

    @Nested
    @DisplayName("resendVerification")
    class ResendVerification {

        @Test
        @DisplayName("creates new token and publishes event when no recent token exists")
        void resendsSuccessfully() {
            User user = buildUser(false);
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
            when(tokenRepository.save(any(EmailVerificationToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            String rawToken = emailVerificationService.resendVerification("alice@example.com");

            assertThat(rawToken).isNotBlank();
            verify(rabbitTemplate).convertAndSend(
                    eq("user.events"),
                    eq("user.registered"),
                    any(Map.class)
            );
        }

        @Test
        @DisplayName("allows resend when existing token is older than 1 minute")
        void allowsResendAfterCooldown() {
            User user = buildUser(false);
            EmailVerificationToken oldToken = EmailVerificationToken.builder()
                    .token("old-token")
                    .user(user)
                    .expiresAt(Instant.now().plus(23, ChronoUnit.HOURS))
                    .createdAt(Instant.now().minus(2, ChronoUnit.MINUTES))
                    .build();

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findByUserId(user.getId())).thenReturn(Optional.of(oldToken));
            when(tokenRepository.save(any(EmailVerificationToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            String rawToken = emailVerificationService.resendVerification("alice@example.com");

            assertThat(rawToken).isNotBlank();
        }

        @Test
        @DisplayName("throws RateLimitExceededException when token was sent less than 1 minute ago")
        void throwsWhenRateLimited() {
            User user = buildUser(false);
            EmailVerificationToken recentToken = EmailVerificationToken.builder()
                    .token("recent-token")
                    .user(user)
                    .expiresAt(Instant.now().plus(23, ChronoUnit.HOURS))
                    .createdAt(Instant.now().minus(30, ChronoUnit.SECONDS))
                    .build();

            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findByUserId(user.getId())).thenReturn(Optional.of(recentToken));

            assertThatThrownBy(() -> emailVerificationService.resendVerification("alice@example.com"))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("wait");
        }

        @Test
        @DisplayName("throws UserNotFoundException when email is not registered")
        void throwsWhenUserNotFound() {
            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> emailVerificationService.resendVerification("nobody@example.com"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("throws EmailAlreadyVerifiedException when user is already verified")
        void throwsWhenAlreadyVerified() {
            User user = buildUser(true);
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> emailVerificationService.resendVerification("alice@example.com"))
                    .isInstanceOf(EmailAlreadyVerifiedException.class);
        }
    }
}
