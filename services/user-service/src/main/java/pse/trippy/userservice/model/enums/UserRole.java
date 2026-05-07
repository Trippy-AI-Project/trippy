package pse.trippy.userservice.model.enums;

/**
 * Represents the role of a user within the Trippy platform.
 * Controls access to protected and admin-only resources.
 */
public enum UserRole {

    /** Standard authenticated user — default role on registration. */
    MEMBER,

    /** User who has created at least one trip. */
    HOST,

    /** Legacy alias for MEMBER, kept for backwards compatibility. */
    USER,

    /** Platform administrator with elevated privileges. */
    ADMIN
}
