package pse.trippy.aiservice.service;

public class AiTimeoutException extends RuntimeException {

    public AiTimeoutException(String message) {
        super(message);
    }
}
