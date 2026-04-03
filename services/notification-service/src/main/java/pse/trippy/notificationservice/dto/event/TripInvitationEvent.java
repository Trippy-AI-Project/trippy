package pse.trippy.notificationservice.dto.event;

public record TripInvitationEvent(
        String tripId,
        String tripTitle,
        String inviterId,
        String inviterName,
        String inviteeEmail,
        String inviteeName
) {
}
