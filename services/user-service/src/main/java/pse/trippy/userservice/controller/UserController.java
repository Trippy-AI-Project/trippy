package pse.trippy.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.userservice.dto.request.UpdateProfileRequest;
import pse.trippy.userservice.model.dto.UserProfileDto;
import pse.trippy.userservice.service.UserService;

import java.util.UUID;

/**
 * REST controller for user profile endpoints.
 *
 * <p>All endpoints require the {@code X-User-Id} header (injected by the API Gateway
 * after JWT validation).
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
}
