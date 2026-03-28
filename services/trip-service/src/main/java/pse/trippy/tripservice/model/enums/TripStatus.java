package pse.trippy.tripservice.model.enums;

/**
 * Lifecycle status of a trip.
 */
public enum TripStatus {

    /** Trip is being planned and is not yet confirmed. */
    DRAFT,

    /** Trip is fully planned and confirmed. */
    PLANNED,

    /** Trip is currently in progress. */
    ONGOING,

    /** Trip has been completed. */
    COMPLETED,

    /** Trip was cancelled before or during execution. */
    CANCELLED
}
