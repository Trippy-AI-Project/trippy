package pse.trippy.notificationservice.dto.response;

public record EmailSentResponse(
        boolean success,
        String message
) {
}
