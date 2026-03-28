package pse.trippy.paymentservice.model.enums;

/**
 * Enum for transaction status.
 * Represents the current state of a payment transaction.
 */
public enum TransactionStatus {
    PENDING("Pending"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    REFUNDED("Refunded");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
