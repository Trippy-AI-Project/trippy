package pse.trippy.chatservice.exception;

/**
 * Exception thrown when a chat room already exists for a trip.
 */
public class ChatRoomAlreadyExistsException extends RuntimeException {

    public ChatRoomAlreadyExistsException(String tripId) {
        super("Chat room already exists for trip: " + tripId);
    }
}
