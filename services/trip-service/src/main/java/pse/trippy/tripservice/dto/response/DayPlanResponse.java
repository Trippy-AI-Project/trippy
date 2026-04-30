package pse.trippy.tripservice.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DayPlanResponse(
        int dayNumber,
        LocalDate date,
        String title,
        List<ActivityResponse> activities
) {
}
