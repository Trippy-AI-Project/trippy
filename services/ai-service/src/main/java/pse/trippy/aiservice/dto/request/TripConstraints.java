package pse.trippy.aiservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDate;
import java.util.Locale;

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
        if (normalized.matches("BUDGET|LOW|MODERATE|MEDIUM|HIGH|PREMIUM|LUXURY")) {
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
            int adults,
            int children
    ) {
    }
}
