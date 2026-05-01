package pse.trippy.aiservice.dto.response;

public record ActivityResponse(
        String time,
        String title,
        String description,
        String location,
        int durationMinutes,
        String category,
        String estimatedCost,
        Double lat,
        Double lng
) {
}
