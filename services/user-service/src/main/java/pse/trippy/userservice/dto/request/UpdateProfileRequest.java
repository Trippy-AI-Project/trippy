package pse.trippy.userservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for PATCH /users/me.
 * All fields are optional — only non-null values are applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 1, max = 100, message = "displayName must be between 1 and 100 characters")
    private String displayName;

    @Size(max = 500, message = "bio must not exceed 500 characters")
    private String bio;

    @Size(max = 20, message = "phoneNumber must not exceed 20 characters")
    private String phoneNumber;
}
