package pse.trippy.chatservice.exception;

/**
 * Thrown when an uploaded file exceeds the maximum allowed size.
 */
public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(String message) {
        super(message);
    }
}
