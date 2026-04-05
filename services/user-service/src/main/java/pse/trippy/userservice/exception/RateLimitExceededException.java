package pse.trippy.userservice.exception;

/**
 * Thrown when a rate-limited action is attempted too frequently (429 Too Many Requests).
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
