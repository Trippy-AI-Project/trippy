package pse.trippy.notificationservice.dto.event;

public record UserRegisteredEvent(
        String userId,
        String email,
        String displayName
) {
}
