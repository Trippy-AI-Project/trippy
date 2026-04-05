package pse.trippy.tripservice.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InviteParticipantRequest(
        @NotNull UUID userId
) {
}
