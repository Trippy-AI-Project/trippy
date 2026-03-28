package pse.trippy.paymentservice.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PaymentMethod entity.
 */
class PaymentMethodTest {

    private PaymentMethod paymentMethod;
    private UUID userId;
    private UUID paymentMethodId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        paymentMethodId = UUID.randomUUID();

        paymentMethod = PaymentMethod.builder()
                .id(paymentMethodId)
                .userId(userId)
                .type("CARD")
                .last4("4242")
                .brand("VISA")
                .isDefault(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testPaymentMethodCreation() {
        assertNotNull(paymentMethod.getId());
        assertEquals(userId, paymentMethod.getUserId());
        assertEquals("CARD", paymentMethod.getType());
        assertEquals("4242", paymentMethod.getLast4());
        assertEquals("VISA", paymentMethod.getBrand());
        assertTrue(paymentMethod.getIsDefault());
        assertNotNull(paymentMethod.getCreatedAt());
    }

    @Test
    void testPaymentMethodWithDifferentBrand() {
        paymentMethod.setBrand("MASTERCARD");
        assertEquals("MASTERCARD", paymentMethod.getBrand());
    }

    @Test
    void testPaymentMethodNotDefault() {
        PaymentMethod pm = PaymentMethod.builder()
                .userId(userId)
                .type("CARD")
                .last4("5555")
                .brand("MASTERCARD")
                .isDefault(false)
                .build();

        assertFalse(pm.getIsDefault());
    }

    @Test
    void testPaymentMethodDefaultIsDefault() {
        PaymentMethod pm = PaymentMethod.builder()
                .userId(userId)
                .type("CARD")
                .last4("1111")
                .brand("AMEX")
                .build();

        assertFalse(pm.getIsDefault());
    }

    @Test
    void testPaymentMethodLast4Digits() {
        paymentMethod.setLast4("9999");
        assertEquals("9999", paymentMethod.getLast4());
    }

    @Test
    void testPaymentMethodType() {
        paymentMethod.setType("BANK_ACCOUNT");
        assertEquals("BANK_ACCOUNT", paymentMethod.getType());
    }

    @Test
    void testPrePersistSetsCreatedAt() {
        PaymentMethod pm = new PaymentMethod();
        pm.prePersist();

        assertNotNull(pm.getCreatedAt());
    }

    @Test
    void testPrePersistDoesNotOverrideCreatedAt() {
        Instant existingTime = Instant.parse("2026-01-01T10:00:00Z");
        PaymentMethod pm = new PaymentMethod();
        pm.setCreatedAt(existingTime);
        pm.prePersist();

        assertEquals(existingTime, pm.getCreatedAt());
    }

    @Test
    void testMultiplePaymentMethods() {
        UUID userId2 = UUID.randomUUID();
        
        PaymentMethod pm1 = PaymentMethod.builder()
                .userId(userId2)
                .type("CARD")
                .last4("1234")
                .brand("VISA")
                .isDefault(true)
                .build();

        PaymentMethod pm2 = PaymentMethod.builder()
                .userId(userId2)
                .type("CARD")
                .last4("5678")
                .brand("MASTERCARD")
                .isDefault(false)
                .build();

        assertEquals(userId2, pm1.getUserId());
        assertEquals(userId2, pm2.getUserId());
        assertTrue(pm1.getIsDefault());
        assertFalse(pm2.getIsDefault());
    }
}
