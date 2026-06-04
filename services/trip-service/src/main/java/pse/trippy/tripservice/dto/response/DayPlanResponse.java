package pse.trippy.tripservice.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DayPlanResponse(
        UUID dayPlanId,
        int dayNumber,
        LocalDate date,
        String title,
        List<ActivityResponse> activities,
        boolean votingEnabled,
        boolean votingFrozen,
        Instant votingDeadline,
        long upvotes,
        long downvotes,
        String currentUserVote
) {
}
