package pse.trippy.tripservice.model.enums;

/**
 * Invite/join status of a participant in a trip.
 */
public enum ParticipantStatus {

    /** Invitation sent; participant has not yet responded. */
    PENDING,

    /** Invite proposed by a participant; awaiting owner approval. */
    PENDING_APPROVAL,

    /** Participant has been invited and has not yet responded. */
    INVITED,

    /** Participant accepted the invitation and is active. */
    ACCEPTED,

    /** Participant declined the invitation. */
    DECLINED,

    /** Participant was accepted but has since left the trip. */
    LEFT
}
