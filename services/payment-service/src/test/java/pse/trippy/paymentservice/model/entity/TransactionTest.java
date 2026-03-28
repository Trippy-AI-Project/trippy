package pse.trippy.paymentservice.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pse.trippy.paymentservice.model.enums.TransactionStatus;
import pse.trippy.paymentservice.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Transaction entity.
 */
class TransactionTest {

    private Transaction transaction;
    private UUID userId;
    private UUID transactionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        transaction = Transaction.builder()
                .id(transactionId)
                .userId(userId)
                .planId("PREMIUM")
                .amount(new BigDecimal("99.99"))
                .currency("EUR")
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.SUBSCRIPTION)
                .description("Premium subscription payment")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testTransactionCreation() {
        assertNotNull(transaction.getId());
        assertEquals(userId, transaction.getUserId());
        assertEquals("PREMIUM", transaction.getPlanId());
        assertEquals(new BigDecimal("99.99"), transaction.getAmount());
        assertEquals("EUR", transaction.getCurrency());
        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
        assertEquals(TransactionType.SUBSCRIPTION, transaction.getType());
        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void testTransactionDefaultCurrency() {
        Transaction txn = Transaction.builder()
                .userId(userId)
                .planId("ENTERPRISE")
                .amount(new BigDecimal("199.99"))
                .status(TransactionStatus.PENDING)
                .type(TransactionType.SUBSCRIPTION)
                .build();

        assertEquals("EUR", txn.getCurrency());
    }

    @Test
    void testTransactionWithDescription() {
        String description = "Annual enterprise subscription";
        transaction.setDescription(description);

        assertEquals(description, transaction.getDescription());
    }

    @Test
    void testTransactionWithNullableDescription() {
        transaction.setDescription(null);
        assertNull(transaction.getDescription());
    }

    @Test
    void testTransactionStatus() {
        transaction.setStatus(TransactionStatus.FAILED);
        assertEquals(TransactionStatus.FAILED, transaction.getStatus());

        transaction.setStatus(TransactionStatus.REFUNDED);
        assertEquals(TransactionStatus.REFUNDED, transaction.getStatus());
    }

    @Test
    void testTransactionType() {
        transaction.setType(TransactionType.REFUND);
        assertEquals(TransactionType.REFUND, transaction.getType());
    }

    @Test
    void testTransactionAmount() {
        BigDecimal newAmount = new BigDecimal("299.99");
        transaction.setAmount(newAmount);
        assertEquals(newAmount, transaction.getAmount());
    }

    @Test
    void testPrePersistSetsCreatedAt() {
        Transaction txn = new Transaction();
        txn.prePersist();

        assertNotNull(txn.getCreatedAt());
    }

    @Test
    void testPrePersistDoesNotOverrideCreatedAt() {
        Instant existingTime = Instant.parse("2026-01-01T10:00:00Z");
        Transaction txn = new Transaction();
        txn.setCreatedAt(existingTime);
        txn.prePersist();

        assertEquals(existingTime, txn.getCreatedAt());
    }
}
