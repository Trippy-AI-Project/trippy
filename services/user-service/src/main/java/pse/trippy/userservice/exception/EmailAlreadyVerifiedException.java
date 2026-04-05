package pse.trippy.userservice.exception;

/**
 * Thrown when a user's email is already verified (409 Conflict).
 */
public class EmailAlreadyVerifiedException extends RuntimeException {

    public EmailAlreadyVerifiedException(String message) {
        super(message);
    }
}
