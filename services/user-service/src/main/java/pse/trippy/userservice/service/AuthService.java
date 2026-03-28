package pse.trippy.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.userservice.dto.request.RegisterRequest;
import pse.trippy.userservice.dto.response.RegisterResponse;
import pse.trippy.userservice.exception.EmailAlreadyExistsException;
import pse.trippy.userservice.model.entity.User;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;
import pse.trippy.userservice.repository.UserRepository;

/**
 * Service handling authentication operations: registration, login, logout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
