package pse.trippy.tripservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record DayPlanRequest(
        @Min(1) int dayNumber,
        LocalDate date,
        @Size(max = 200) String title,
        @NotNull @Valid List<ActivityRequest> activities
) {
}
