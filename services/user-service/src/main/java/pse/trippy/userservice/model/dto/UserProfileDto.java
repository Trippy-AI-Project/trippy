package pse.trippy.userservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Data Transfer Object representing a user's profile information.
 * Used in authentication responses as per the API contract.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private UUID userId;
    private String email;
    private String displayName;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String bio;
    private String country;
    private String phoneNumber;
    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean isVerifiedHost;
    // createdAt, updatedAt omitted for brevity, but easily added later if needed.
}
