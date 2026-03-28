package pse.trippy.paymentservice.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransactionType enum.
 */
class TransactionTypeTest {

    @Test
    void testTransactionTypeValues() {
        assertEquals("Subscription Payment", TransactionType.SUBSCRIPTION.getDisplayName());
        assertEquals("Refund", TransactionType.REFUND.getDisplayName());
    }

    @Test
    void testTransactionTypeEnum() {
        assertNotNull(TransactionType.SUBSCRIPTION);
        assertNotNull(TransactionType.REFUND);

        assertEquals(2, TransactionType.values().length);
    }

    @Test
    void testTransactionTypeValueOf() {
        assertEquals(TransactionType.SUBSCRIPTION, TransactionType.valueOf("SUBSCRIPTION"));
        assertEquals(TransactionType.REFUND, TransactionType.valueOf("REFUND"));
    }

    @Test
    void testTransactionTypeName() {
        assertEquals("SUBSCRIPTION", TransactionType.SUBSCRIPTION.name());
        assertEquals("REFUND", TransactionType.REFUND.name());
    }
}
