package pse.trippy.userservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Response body for {@code POST /auth/login}.
 */
@Getter
@Builder
@AllArgsConstructor
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final int expiresIn;
    private final UserProfileDto user;

    @Builder.Default
    private final String tokenType = "Bearer";
}
