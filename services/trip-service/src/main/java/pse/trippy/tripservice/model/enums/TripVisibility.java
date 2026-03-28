package pse.trippy.tripservice.model.enums;

/**
 * Visibility level of a trip in the discovery feed.
 */
public enum TripVisibility {

    /** Trip is visible only to the owner and invited participants. */
    PRIVATE,

    /** Trip is visible to all users in the discovery feed. */
    PUBLIC,

    /** Trip is accessible via direct link but not listed in the feed. */
    UNLISTED
}
