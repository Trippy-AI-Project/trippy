package pse.trippy.chatservice.exception;

/**
 * Thrown when a chat message cannot be found.
 */
public class MessageNotFoundException extends RuntimeException {
    public MessageNotFoundException(String message) {
        super(message);
    }
}
