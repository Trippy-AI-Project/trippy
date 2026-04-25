package pse.trippy.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.userservice.config.RabbitMqConfig;
import pse.trippy.userservice.exception.EmailAlreadyVerifiedException;
import pse.trippy.userservice.exception.InvalidTokenException;
import pse.trippy.userservice.exception.RateLimitExceededException;
import pse.trippy.userservice.exception.UserNotFoundException;
import pse.trippy.userservice.exception.VerificationTokenExpiredException;
import pse.trippy.userservice.model.entity.EmailVerificationToken;
import pse.trippy.userservice.model.entity.User;
import pse.trippy.userservice.repository.EmailVerificationTokenRepository;
import pse.trippy.userservice.repository.UserRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service handling email verification workflows: token creation, validation, and resend.
 *
 * <p>Verification tokens are stored as plaintext UUIDs with a 24-hour expiry.
 * Successful verification publishes a {@code user.email.verified} event to RabbitMQ.
 */
@Service
@Slf4j
public class EmailVerificationService {

    private static final long TOKEN_EXPIRY_HOURS = 24;
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            RabbitTemplate rabbitTemplate) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Creates a new email verification token for the given user.
     * Any existing tokens for the user are deleted first.
     *
     * @param user the user to generate a verification token for
     * @return the raw token string (UUID)
     */
    @Transactional
    public String createVerificationToken(User user) {
        tokenRepository.deleteAllByUserId(user.getId());

        String rawToken = UUID.randomUUID().toString();

        EmailVerificationToken token = EmailVerificationToken.builder()
                .token(rawToken)
                .user(user)
                .expiresAt(Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))
                .build();

        tokenRepository.save(token);
        log.info("Verification token created for userId={}", user.getId());

        return rawToken;
    }

    /**
     * Verifies a user's email using the provided token.
     *
     * @param rawToken the verification token
     * @throws InvalidTokenException            if the token is not found
     * @throws VerificationTokenExpiredException if the token has expired
     * @throws EmailAlreadyVerifiedException     if the user is already verified
     */
    @Transactional
    public void verifyEmail(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw new VerificationTokenExpiredException("Verification token has expired");
        }

        User user = token.getUser();

        if (user.isEmailVerified()) {
            tokenRepository.delete(token);
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        tokenRepository.deleteAllByUserId(user.getId());

        log.info("Email verified for userId={}", user.getId());
        publishEmailVerifiedEvent(user);
    }

    /**
     * Resends the verification email for the given email address.
     * Rate-limited to one request per minute.
     *
     * @param email the user's email address
     * @return the new raw verification token
     * @throws UserNotFoundException         if the email is not registered
     * @throws EmailAlreadyVerifiedException if the user is already verified
     * @throws RateLimitExceededException    if a token was sent less than 1 minute ago
     */
    @Transactional
    public String resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        if (user.isEmailVerified()) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }

        Optional<EmailVerificationToken> existing = tokenRepository.findByUserId(user.getId());
        if (existing.isPresent()) {
            Instant createdAt = existing.get().getCreatedAt();
            if (createdAt != null && createdAt.plus(RESEND_COOLDOWN).isAfter(Instant.now())) {
                throw new RateLimitExceededException(
                        "Verification email already sent recently. Please wait before requesting again.");
            }
        }

        String rawToken = createVerificationToken(user);
        log.info("Verification email resent for userId={}", user.getId());

        publishResendVerificationEvent(user, rawToken);
        return rawToken;
    }

    private void publishEmailVerifiedEvent(User user) {
        Map<String, Object> event = Map.of(
                "eventType", "user.email.verified",
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName(),
                "timestamp", Instant.now().toString()
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.USER_EVENTS_EXCHANGE,
                    "user.email.verified",
                    event
            );
            log.info("Published user.email.verified event for userId={}", user.getId());
        } catch (AmqpException ex) {
            log.error("Failed to publish user.email.verified event for userId={}", user.getId(), ex);
        }
    }

    private void publishResendVerificationEvent(User user, String token) {
        Map<String, Object> event = Map.of(
                "eventType", "user.registered",
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName(),
                "verificationToken", token,
                "timestamp", Instant.now().toString()
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.USER_EVENTS_EXCHANGE,
                    "user.registered",
                    event
            );
            log.info("Published user.registered event (resend) for userId={}", user.getId());
        } catch (AmqpException ex) {
            log.error("Failed to publish resend verification event for userId={}", user.getId(), ex);
        }
    }
}
