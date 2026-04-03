package pse.trippy.paymentservice.model.enums;

/**
 * Enum for transaction type.
 * Represents the category or purpose of a payment transaction.
 */
public enum TransactionType {
    SUBSCRIPTION("Subscription Payment"),
    REFUND("Refund");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
