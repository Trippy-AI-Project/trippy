package pse.trippy.tripservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record VoteSummaryResponse(
        UUID dayPlanId,
        int dayNumber,
        long upvotes,
        long downvotes,
        String currentUserVote,
        boolean votingEnabled,
        boolean votingFrozen,
        Instant votingDeadline
) {
}
