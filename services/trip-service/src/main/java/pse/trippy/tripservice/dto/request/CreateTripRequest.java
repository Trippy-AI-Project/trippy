package pse.trippy.tripservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTripRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "Destination is required")
        String destination,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        LocalDate startDate,

        LocalDate endDate,

        String visibility,

        @Min(value = 1, message = "Must allow at least 1 participant")
        @Max(value = 20, message = "Maximum 20 participants allowed")
        Integer maxParticipants
) {
}
