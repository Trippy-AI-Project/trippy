package pse.trippy.userservice.exception;

/**
 * Exception thrown when attempting to register with an email that already exists.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
