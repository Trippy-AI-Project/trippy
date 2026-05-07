package pse.trippy.aiservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.Locale;

public record TripConstraints(
        @NotBlank(message = "Destination is required")
        String destination,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        String budgetLevel,

        @Valid
        Travelers travelers,

        String accommodationType
) {
    @AssertTrue(message = "End date must be on or after start date")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "Budget must be positive or a supported budget level")
    public boolean isBudgetValid() {
        if (budgetLevel == null || budgetLevel.isBlank()) {
            return true;
        }

        String normalized = budgetLevel.trim().toUpperCase(Locale.ROOT);
        if (normalized.matches("BUDGET|ECONOMY|LOW|MODERATE|MEDIUM|HIGH|PREMIUM|LUXURY")) {
            return true;
        }
        if (normalized.contains("-")) {
            return false;
        }

        String numeric = normalized.replaceAll("[^0-9.]", "");
        if (numeric.isBlank()) {
            return false;
        }
        try {
            return Double.parseDouble(numeric) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public record Travelers(
            @Min(value = 1, message = "At least one adult is required")
            int adults,
            @Min(value = 0, message = "Children cannot be negative")
            int children
    ) {
    }
}
