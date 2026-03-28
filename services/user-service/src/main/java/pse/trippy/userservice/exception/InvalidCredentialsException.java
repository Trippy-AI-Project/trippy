package pse.trippy.userservice.exception;

/**
 * Thrown when login credentials (email or password) are invalid.
 * Maps to HTTP 401 Unauthorized.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
