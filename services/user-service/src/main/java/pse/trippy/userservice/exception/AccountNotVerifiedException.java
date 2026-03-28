package pse.trippy.userservice.exception;

/**
 * Thrown when an inactive or unverified account attempts to log in.
 * Maps to HTTP 403 Forbidden.
 */
public class AccountNotVerifiedException extends RuntimeException {
    public AccountNotVerifiedException(String message) {
        super(message);
    }
}
