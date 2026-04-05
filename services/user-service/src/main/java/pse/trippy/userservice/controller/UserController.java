package pse.trippy.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.userservice.dto.request.ResendVerificationRequest;
import pse.trippy.userservice.dto.request.UpdateProfileRequest;
import pse.trippy.userservice.dto.request.VerifyEmailRequest;
import pse.trippy.userservice.model.dto.UserProfileDto;
import pse.trippy.userservice.service.EmailVerificationService;
import pse.trippy.userservice.service.UserService;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user profile and email verification endpoints.
 *
 * <p>Profile endpoints require the {@code X-User-Id} header (injected by the API Gateway
 * after JWT validation). Verification endpoints are public.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;

    /**
     * Returns the authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    /**
     * Partially updates the authenticated user's profile.
     */
    @PatchMapping("/me")
    public ResponseEntity<UserProfileDto> updateMyProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    /**
     * Verifies a user's email address using the provided token.
     *
     * @param request the verification request containing the token
     * @return 200 OK with a success message
     */
    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verifyEmail(request.getToken());
        return ResponseEntity.ok(Map.of("message", "Email verified successfully."));
    }

    /**
     * Resends the verification email to the specified address.
     * Rate-limited to one request per minute per user.
     *
     * @param request the resend request containing the email
     * @return 200 OK with a success message
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendVerification(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Verification email sent."));
    }
}
