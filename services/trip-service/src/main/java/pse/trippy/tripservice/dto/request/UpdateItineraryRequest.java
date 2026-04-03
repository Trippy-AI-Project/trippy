package pse.trippy.tripservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateItineraryRequest(
        @NotNull @Valid List<DayPlanRequest> dayPlans
) {
}
