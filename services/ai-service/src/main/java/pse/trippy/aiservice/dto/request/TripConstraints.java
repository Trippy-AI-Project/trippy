package pse.trippy.aiservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TripConstraints(
        @NotBlank(message = "Destination is required")
        String destination,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        String budgetLevel,

        Travelers travelers,

        String accommodationType
) {
    public record Travelers(
            int adults,
            int children
    ) {
    }
}
