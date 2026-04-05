package pse.trippy.userservice.exception;

/**
 * Thrown when an email verification token has expired (410 Gone).
 */
public class VerificationTokenExpiredException extends RuntimeException {

    public VerificationTokenExpiredException(String message) {
        super(message);
    }
}
