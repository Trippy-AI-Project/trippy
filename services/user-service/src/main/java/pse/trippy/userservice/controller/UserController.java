package pse.trippy.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.userservice.dto.request.UpdateProfileRequest;
import pse.trippy.userservice.dto.response.UserProfileResponse;
import pse.trippy.userservice.service.UserProfileService;

import java.util.UUID;

/**
 * REST controller for user profile management.
 *
 * <p>User identity is provided by the API Gateway via the {@code X-User-Id} header,
 * which is injected after JWT validation. Requests without this header return 401.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    /**
     * Returns the authenticated user's full profile.
     *
     * @param userId the user ID injected by the gateway (X-User-Id header)
     * @return 200 with profile, or 401 if X-User-Id is absent
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(userProfileService.getProfile(UUID.fromString(userId)));
    }

    /**
     * Partially updates the authenticated user's profile.
     * Only non-null fields are applied.
     *
     * @param userId  the user ID injected by the gateway (X-User-Id header)
     * @param request the fields to update
     * @return 200 with updated profile, or 401 if X-User-Id is absent
     */
    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(userProfileService.updateProfile(UUID.fromString(userId), request));
    }
}
