package pse.trippy.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pse.trippy.userservice.model.enums.SubscriptionPlan;
import pse.trippy.userservice.model.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for user profile endpoints (GET /users/me, PATCH /users/me).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String displayName;
    private String bio;
    private String phoneNumber;
    private String avatarUrl;
    private UserRole role;
    private SubscriptionPlan plan;
    private boolean emailVerified;
    private Instant createdAt;
}
