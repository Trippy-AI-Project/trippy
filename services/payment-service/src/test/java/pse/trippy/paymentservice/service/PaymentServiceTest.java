package pse.trippy.paymentservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pse.trippy.paymentservice.dto.CheckoutRequestDto;
import pse.trippy.paymentservice.dto.CheckoutResponseDto;
import pse.trippy.paymentservice.dto.PlanDto;
import pse.trippy.paymentservice.model.entity.Transaction;
import pse.trippy.paymentservice.model.enums.TransactionStatus;
import pse.trippy.paymentservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PaymentService.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UUID userId;
    private CheckoutRequestDto checkoutRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        checkoutRequest = CheckoutRequestDto.builder()
                .planId("PREMIUM")
                .paymentMethodId("pm_test_123")
                .build();
    }

    @Test
    void testGetAvailablePlans() {
        List<PlanDto> plans = paymentService.getAvailablePlans();

        assertNotNull(plans);
        assertEquals(2, plans.size());

        // Check PREMIUM plan
        PlanDto premiumPlan = plans.stream()
                .filter(p -> "PREMIUM".equals(p.getPlanId()))
                .findFirst()
                .orElse(null);
        assertNotNull(premiumPlan);
        assertEquals(new BigDecimal("9.99"), premiumPlan.getPrice());
        assertEquals("EUR", premiumPlan.getCurrency());
        assertTrue(premiumPlan.getFeatures().contains("10 trips"));
        assertEquals("monthly", premiumPlan.getBillingCycle());

        // Check ENTERPRISE plan
        PlanDto enterprisePlan = plans.stream()
                .filter(p -> "ENTERPRISE".equals(p.getPlanId()))
                .findFirst()
                .orElse(null);
        assertNotNull(enterprisePlan);
        assertEquals(new BigDecimal("29.99"), enterprisePlan.getPrice());
        assertEquals("EUR", enterprisePlan.getCurrency());
        assertTrue(enterprisePlan.getFeatures().contains("Unlimited trips"));
    }

    @Test
    void testCheckoutSuccess() {
        // Arrange
        Transaction expectedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .planId("PREMIUM")
                .amount(new BigDecimal("9.99"))
                .currency("EUR")
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(expectedTransaction);

        // Act
        CheckoutResponseDto response = paymentService.checkout(userId, checkoutRequest);

        // Assert
        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("PREMIUM", response.getPlan());
        assertEquals(new BigDecimal("9.99"), response.getAmount().getValue());
        assertEquals("EUR", response.getAmount().getCurrency());
        assertEquals("Subscription activated successfully", response.getMessage());

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testCheckoutWithInvalidPlan() {
        CheckoutRequestDto invalidRequest = CheckoutRequestDto.builder()
                .planId("INVALID_PLAN")
                .paymentMethodId("pm_test_123")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.checkout(userId, invalidRequest);
        });

        verify(transactionRepository, times(0)).save(any(Transaction.class));
    }

    @Test
    void testCheckoutWithPremiumPlan() {
        Transaction premiumTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .planId("PREMIUM")
                .amount(new BigDecimal("9.99"))
                .currency("EUR")
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(premiumTransaction);

        CheckoutResponseDto response = paymentService.checkout(userId, checkoutRequest);

        assertEquals("PREMIUM", response.getPlan());
        assertEquals(new BigDecimal("9.99"), response.getAmount().getValue());
    }

    @Test
    void testCheckoutWithEnterprisePlan() {
        CheckoutRequestDto enterpriseRequest = CheckoutRequestDto.builder()
                .planId("ENTERPRISE")
                .paymentMethodId("pm_test_456")
                .build();

        Transaction enterpriseTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .planId("ENTERPRISE")
                .amount(new BigDecimal("29.99"))
                .currency("EUR")
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(enterpriseTransaction);

        CheckoutResponseDto response = paymentService.checkout(userId, enterpriseRequest);

        assertEquals("ENTERPRISE", response.getPlan());
        assertEquals(new BigDecimal("29.99"), response.getAmount().getValue());
    }

    @Test
    void testCheckoutTransactionPersisted() {
        Transaction persistedTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .planId("PREMIUM")
                .amount(new BigDecimal("9.99"))
                .currency("EUR")
                .status(TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.save(any(Transaction.class)))
                .thenReturn(persistedTransaction);

        CheckoutResponseDto response = paymentService.checkout(userId, checkoutRequest);

        assertNotNull(response.getTransactionId());
        assertEquals(persistedTransaction.getId(), response.getTransactionId());
    }
}
