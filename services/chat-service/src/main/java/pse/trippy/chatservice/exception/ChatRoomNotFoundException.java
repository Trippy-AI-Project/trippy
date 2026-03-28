package pse.trippy.chatservice.exception;

/**
 * Exception thrown when a chat room is not found.
 */
public class ChatRoomNotFoundException extends RuntimeException {

    public ChatRoomNotFoundException(String tripId) {
        super("Chat room not found for trip: " + tripId);
    }
}
