package pse.trippy.aiservice.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TripConstraints(
        @NotBlank(message = "Destination is required")
        String destination,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date must be today or in the future")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        @FutureOrPresent(message = "End date must be today or in the future")
        LocalDate endDate,

        String budgetLevel,

        Travelers travelers,

        String accommodationType,

        @DecimalMin(value = "0.01", message = "Budget must be greater than 0")
        BigDecimal budget
) {
    public TripConstraints(String destination, LocalDate startDate, LocalDate endDate,
                           String budgetLevel, Travelers travelers, String accommodationType) {
        this(destination, startDate, endDate, budgetLevel, travelers, accommodationType, null);
    }

    @AssertTrue(message = "End date must be on or after start date")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    public record Travelers(
            @Min(value = 1, message = "At least one adult is required")
            int adults,

            @Min(value = 0, message = "Children cannot be negative")
            int children
    ) {
    }
}
