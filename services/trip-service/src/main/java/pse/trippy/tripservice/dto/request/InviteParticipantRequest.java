package pse.trippy.tripservice.dto.request;

import java.util.UUID;

public record InviteParticipantRequest(
        UUID userId,
        String email,
        String message
) {
}
