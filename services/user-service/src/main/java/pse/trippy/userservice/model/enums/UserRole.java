package pse.trippy.userservice.model.enums;

/**
 * Represents the role of a user within the Trippy platform.
 * Controls access to protected and admin-only resources.
 */
public enum UserRole {

    /** Standard authenticated user. */
    USER,

    /** Platform administrator with elevated privileges. */
    ADMIN
}
