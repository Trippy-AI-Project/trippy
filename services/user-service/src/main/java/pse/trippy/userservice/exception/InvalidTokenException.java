package pse.trippy.userservice.exception;

/**
 * Thrown when a refresh token is invalid, expired, or not found.
 * Maps to HTTP 401 Unauthorized.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
