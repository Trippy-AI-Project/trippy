package pse.trippy.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight public profile response for batch user lookups.
 * Excludes sensitive fields like email, phone, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicProfileResponse {

    private UUID id;
    private String displayName;
    private String avatarUrl;
    private String country;
}
