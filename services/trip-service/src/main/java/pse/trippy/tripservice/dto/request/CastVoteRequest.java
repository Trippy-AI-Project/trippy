package pse.trippy.tripservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record CastVoteRequest(
        @NotNull String voteType
) {
}
