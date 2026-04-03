package pse.trippy.paymentservice.exception;

public class InvalidPlanException extends RuntimeException {
    public InvalidPlanException(String planId) {
        super("Invalid plan ID: " + planId);
    }
}
