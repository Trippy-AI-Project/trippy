package pse.trippy.aiservice.service;

public class AiServiceUnavailableException extends RuntimeException {
    public AiServiceUnavailableException(String message) {
        super(message);
    }
}
