package pse.trippy.aiservice.dto.response;

public record ErrorResponse(
        int status,
        String message
) {
}
