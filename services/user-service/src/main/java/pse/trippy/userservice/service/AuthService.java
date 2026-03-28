package pse.trippy.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import pse.trippy.userservice.model.dto.UserProfileDto;
import pse.trippy.userservice.model.entity.RefreshToken;
import pse.trippy.userservice.model.entity.User;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;
import pse.trippy.userservice.repository.RefreshTokenRepository;
import pse.trippy.userservice.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

/**
 * Service handling authentication operations: registration, login, logout.
 *
 * <p>Handles user authentication workflows: login and refresh-token rotation.
 * Refresh tokens are stored as SHA-256 hashes in the database.
 */
@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final int refreshTokenExpiryDays;
    private final int rememberMeExpiryDays;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${trippy.jwt.refresh-token-expiry-days}") int refreshTokenExpiryDays,
            @Value("${trippy.jwt.remember-me-expiry-days}") int rememberMeExpiryDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
        this.rememberMeExpiryDays = rememberMeExpiryDays;
    }

    /**
     * Registers a new user with the provided credentials.
     *
     * @param request the registration request containing email, password, and displayName
     * @return response with userId and email
     * @throws EmailAlreadyExistsException if email is already registered
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email already exists - {}", request.getEmail());
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Create user entity with hashed password
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .role(UserRole.USER)
                .plan(SubscriptionPlan.FREE)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .message("Registration successful. Please verify your email.")
                .verificationRequired(true)
                .build();
    }

    /**
     * Authenticates the user and returns an access/refresh token pair.
     *
     * @param request login credentials
     * @return tokens and expiry metadata
     * @throws InvalidCredentialsException if email is unknown or password is wrong
     * @throws AccountNotVerifiedException if the account is not verified (403 Forbidden)
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            throw new AccountNotVerifiedException("Account not verified. Please verify your email.");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = createRefreshToken(user, request.isRememberMe());

        log.info("User logged in: userId={}", user.getId());

        UserProfileDto userProfileDto = UserProfileDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .phoneNumber(user.getPhoneNumber())
                .emailVerified(user.isEmailVerified())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .user(userProfileDto)
                .build();
    }

    /**
     * Validates the provided refresh token, rotates it (delete old → create new),
     * and returns a new access/refresh token pair.
     *
     * @param request contains the raw refresh token
     * @return new tokens and expiry metadata
     * @throws InvalidTokenException if the token is unknown or expired
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String hashedToken = hashToken(request.getRefreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (storedToken.isExpired()) {
            refreshTokenRepository.deleteByTokenValue(hashedToken);
            throw new InvalidTokenException("Refresh token has expired");
        }

        User user = storedToken.getUser();
        boolean rememberMe = storedToken.isRememberMe();

        // Atomic delete guards against concurrent reuse (race condition)
        int deleted = refreshTokenRepository.deleteByTokenValue(hashedToken);
        if (deleted == 0) {
            throw new InvalidTokenException("Refresh token already consumed");
        }

        String newRawRefreshToken = createRefreshToken(user, rememberMe);
        String accessToken = jwtService.generateAccessToken(user);

        log.info("Refresh token rotated: userId={}", user.getId());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRawRefreshToken)
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .build();
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    /**
     * Creates a new refresh token, persists its SHA-256 hash, and returns the raw value.
     */
    private String createRefreshToken(User user, boolean rememberMe) {
        String rawToken = UUID.randomUUID().toString();
        String hashedToken = hashToken(rawToken);
        int expiryDays = rememberMe ? rememberMeExpiryDays : refreshTokenExpiryDays;

        RefreshToken refreshToken = RefreshToken.builder()
                .token(hashedToken)
                .user(user)
                .rememberMe(rememberMe)
                .expiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Computes the SHA-256 hash of a token and returns it as a Base64-encoded string.
     */
    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
