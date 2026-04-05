package pse.trippy.paymentservice.exception;

public class InvalidPaymentException extends RuntimeException {
    public InvalidPaymentException(String message) {
        super(message);
    }
}
