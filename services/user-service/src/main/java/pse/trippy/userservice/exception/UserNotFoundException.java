package pse.trippy.userservice.exception;

/**
 * Thrown when a requested user cannot be found.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
