package pse.trippy.tripservice.dto.response;

import java.time.LocalTime;

public record ActivityResponse(
        String title,
        String description,
        String location,
        LocalTime startTime,
        LocalTime endTime,
        String category,
        String notes,
        int orderIndex
) {
}
