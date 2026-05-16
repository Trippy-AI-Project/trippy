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

    /**
     * Deprecated: previously the default role before MEMBER was introduced.
     * Treated equivalently to MEMBER in all role checks.
     * Kept for DB backwards compatibility — do not use for new records.
     */
    @Deprecated
    USER,

    /** Platform administrator with elevated privileges. */
    ADMIN
}
