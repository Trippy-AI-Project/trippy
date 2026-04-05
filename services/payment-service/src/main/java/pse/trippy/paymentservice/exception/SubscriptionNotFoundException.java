package pse.trippy.paymentservice.exception;

public class SubscriptionNotFoundException extends RuntimeException {
    public SubscriptionNotFoundException(String message) {
        super(message);
    }
}
