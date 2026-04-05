package pse.trippy.chatservice.exception;

/**
 * Thrown when an uploaded file has an unsupported content type.
 */
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
