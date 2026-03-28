package pse.trippy.userservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Response body for {@code POST /auth/refresh}.
 */
@Getter
@Builder
@AllArgsConstructor
public class TokenResponse {

    private final String accessToken;
    private final String refreshToken;
    private final int expiresIn;
}
