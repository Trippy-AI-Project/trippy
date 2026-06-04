package pse.trippy.tripservice.dto.request;

import java.time.Instant;

public record VotingSettingsRequest(
        boolean votingEnabled,
        Instant votingDeadline
) {
}
