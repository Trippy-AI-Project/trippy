package pse.trippy.tripservice.model.enums;

/**
 * Role of a participant within a trip.
 */
public enum ParticipantRole {

    /** Created the trip; has full control including deletion. */
    OWNER,

    /** Can edit trip details, itinerary, and manage participants. */
    EDITOR,

    /** Read-only access; can view all trip content. */
    VIEWER,

    /** Standard trip member; can view and participate. */
    MEMBER
}
