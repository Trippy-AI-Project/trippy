package pse.trippy.userservice.exception;

import java.util.UUID;

/**
 * Thrown when a user with the given ID cannot be found.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID userId) {
        super("User not found: " + userId);
    }
}
