package pse.trippy.paymentservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pse.trippy.paymentservice.dto.CheckoutRequestDto;
import pse.trippy.paymentservice.dto.CheckoutResponseDto;
import pse.trippy.paymentservice.dto.PlanDto;
import pse.trippy.paymentservice.service.PaymentService;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PaymentController.
 */
@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private UUID userId;
    private Principal principal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn(userId.toString());
    }

    @Test
    void testGetPlans() {
        List<PlanDto> plans = new ArrayList<>();
        plans.add(PlanDto.builder()
                .planId("PREMIUM")
                .price(new BigDecimal("9.99"))
                .currency("EUR")
                .features("Up to 10 trips, AI itineraries, priority support")
                .billingCycle("monthly")
                .build());
        plans.add(PlanDto.builder()
                .planId("ENTERPRISE")
                .price(new BigDecimal("29.99"))
                .currency("EUR")
                .features("Unlimited trips, advanced AI, team features")
                .billingCycle("monthly")
                .build());

        when(paymentService.getAvailablePlans()).thenReturn(plans);

        ResponseEntity<List<PlanDto>> response = paymentController.getPlans();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void testCheckoutSuccess() {
        CheckoutRequestDto request = CheckoutRequestDto.builder()
                .planId("PREMIUM")
                .paymentMethodId("pm_test_123")
                .build();

        CheckoutResponseDto expectedResponse = CheckoutResponseDto.builder()
                .transactionId(UUID.randomUUID())
                .status("COMPLETED")
                .plan("PREMIUM")
                .amount(CheckoutResponseDto.AmountDto.builder()
                        .value(new BigDecimal("9.99"))
                        .currency("EUR")
                        .build())
                .message("Subscription activated successfully")
                .build();

        when(paymentService.checkout(eq(userId), eq(request)))
                .thenReturn(expectedResponse);

        ResponseEntity<?> response = paymentController.checkout(request, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof CheckoutResponseDto);

        CheckoutResponseDto body = (CheckoutResponseDto) response.getBody();
        assertEquals("COMPLETED", body.getStatus());
        assertEquals("PREMIUM", body.getPlan());
        assertEquals(new BigDecimal("9.99"), body.getAmount().getValue());
    }

    @Test
    void testCheckoutWithoutPlanId() {
        CheckoutRequestDto request = CheckoutRequestDto.builder()
                .planId(null)
                .paymentMethodId("pm_test_123")
                .build();

        ResponseEntity<?> response = paymentController.checkout(request, principal);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof PaymentController.ErrorResponse);
    }

    @Test
    void testCheckoutWithoutPaymentMethodId() {
        CheckoutRequestDto request = CheckoutRequestDto.builder()
                .planId("PREMIUM")
                .paymentMethodId(null)
                .build();

        ResponseEntity<?> response = paymentController.checkout(request, principal);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof PaymentController.ErrorResponse);
    }

    @Test
    void testCheckoutWithInvalidPlan() {
        CheckoutRequestDto request = CheckoutRequestDto.builder()
                .planId("INVALID_PLAN")
                .paymentMethodId("pm_test_123")
                .build();

        when(paymentService.checkout(eq(userId), eq(request)))
                .thenThrow(new IllegalArgumentException("Invalid plan ID: INVALID_PLAN"));

        ResponseEntity<?> response = paymentController.checkout(request, principal);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof PaymentController.ErrorResponse);
    }

    @Test
    void testCheckoutWithEnterprisePlan() {
        CheckoutRequestDto request = CheckoutRequestDto.builder()
                .planId("ENTERPRISE")
                .paymentMethodId("pm_test_456")
                .build();

        CheckoutResponseDto expectedResponse = CheckoutResponseDto.builder()
                .transactionId(UUID.randomUUID())
                .status("COMPLETED")
                .plan("ENTERPRISE")
                .amount(CheckoutResponseDto.AmountDto.builder()
                        .value(new BigDecimal("29.99"))
                        .currency("EUR")
                        .build())
                .message("Subscription activated successfully")
                .build();

        when(paymentService.checkout(eq(userId), eq(request)))
                .thenReturn(expectedResponse);

        ResponseEntity<?> response = paymentController.checkout(request, principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        CheckoutResponseDto body = (CheckoutResponseDto) response.getBody();
        assertEquals("ENTERPRISE", body.getPlan());
        assertEquals(new BigDecimal("29.99"), body.getAmount().getValue());
    }
}
