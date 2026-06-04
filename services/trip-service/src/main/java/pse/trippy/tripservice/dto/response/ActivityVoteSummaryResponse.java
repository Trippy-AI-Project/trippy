package pse.trippy.tripservice.dto.response;

import java.util.UUID;

public record ActivityVoteSummaryResponse(
        UUID activityId,
        long upvotes,
        long downvotes,
        String currentUserVote
) {
}
