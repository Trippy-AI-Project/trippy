package pse.trippy.paymentservice.model.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TransactionStatus enum.
 */
class TransactionStatusTest {

    @Test
    void testTransactionStatusValues() {
        assertEquals("Pending", TransactionStatus.PENDING.getDisplayName());
        assertEquals("Completed", TransactionStatus.COMPLETED.getDisplayName());
        assertEquals("Failed", TransactionStatus.FAILED.getDisplayName());
        assertEquals("Refunded", TransactionStatus.REFUNDED.getDisplayName());
    }

    @Test
    void testTransactionStatusEnum() {
        assertNotNull(TransactionStatus.PENDING);
        assertNotNull(TransactionStatus.COMPLETED);
        assertNotNull(TransactionStatus.FAILED);
        assertNotNull(TransactionStatus.REFUNDED);

        assertEquals(4, TransactionStatus.values().length);
    }

    @Test
    void testTransactionStatusValueOf() {
        assertEquals(TransactionStatus.PENDING, TransactionStatus.valueOf("PENDING"));
        assertEquals(TransactionStatus.COMPLETED, TransactionStatus.valueOf("COMPLETED"));
        assertEquals(TransactionStatus.FAILED, TransactionStatus.valueOf("FAILED"));
        assertEquals(TransactionStatus.REFUNDED, TransactionStatus.valueOf("REFUNDED"));
    }

    @Test
    void testTransactionStatusName() {
        assertEquals("PENDING", TransactionStatus.PENDING.name());
        assertEquals("COMPLETED", TransactionStatus.COMPLETED.name());
        assertEquals("FAILED", TransactionStatus.FAILED.name());
        assertEquals("REFUNDED", TransactionStatus.REFUNDED.name());
    }
}
