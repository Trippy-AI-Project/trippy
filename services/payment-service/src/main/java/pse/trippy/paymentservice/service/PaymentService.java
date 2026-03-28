package pse.trippy.paymentservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.paymentservice.dto.CheckoutRequestDto;
import pse.trippy.paymentservice.dto.CheckoutResponseDto;
import pse.trippy.paymentservice.dto.PlanDto;
import pse.trippy.paymentservice.model.entity.Transaction;
import pse.trippy.paymentservice.model.enums.PlanDetails;
import pse.trippy.paymentservice.model.enums.TransactionStatus;
import pse.trippy.paymentservice.model.enums.TransactionType;
import pse.trippy.paymentservice.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for payment operations.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final TransactionRepository transactionRepository;

    /**
     * Get all available subscription plans.
     *
     * @return list of available plans
     */
    public List<PlanDto> getAvailablePlans() {
        List<PlanDto> plans = new ArrayList<>();
        
        for (PlanDetails plan : PlanDetails.values()) {
            plans.add(PlanDto.builder()
                    .planId(plan.getPlanId())
                    .price(plan.getPrice())
                    .currency("EUR")
                    .features(plan.getFeatures())
                    .billingCycle("monthly")
                    .build());
        }
        
        return plans;
    }

    /**
     * Process checkout for a subscription plan.
     *
     * @param userId the user ID
     * @param request the checkout request
     * @return checkout response with transaction details
     * @throws IllegalArgumentException if plan is invalid
     */
    @Transactional
    public CheckoutResponseDto checkout(UUID userId, CheckoutRequestDto request) {
        // Validate plan
        PlanDetails plan = PlanDetails.fromId(request.getPlanId());
        if (plan == null) {
            throw new IllegalArgumentException("Invalid plan ID: " + request.getPlanId());
        }

        // Dummy payment verification (always succeeds for now)
        boolean paymentSucceeded = verifyPayment(userId, request.getPaymentMethodId());

        if (!paymentSucceeded) {
            throw new IllegalArgumentException("Payment verification failed");
        }

        // Create and persist transaction
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .planId(request.getPlanId())
                .amount(plan.getPrice())
                .currency("EUR")
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.SUBSCRIPTION)
                .description("Subscription upgrade to " + request.getPlanId())
                .createdAt(Instant.now())
                .build();

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Build response
        return CheckoutResponseDto.builder()
                .transactionId(savedTransaction.getId())
                .status("COMPLETED")
                .plan(savedTransaction.getPlanId())
                .amount(CheckoutResponseDto.AmountDto.builder()
                        .value(savedTransaction.getAmount())
                        .currency(savedTransaction.getCurrency())
                        .build())
                .message("Subscription activated successfully")
                .build();
    }

    /**
     * Verify payment (dummy implementation - always succeeds).
     *
     * @param userId the user ID
     * @param paymentMethodId the payment method ID
     * @return true if payment is verified
     */
    private boolean verifyPayment(UUID userId, String paymentMethodId) {
        // Dummy implementation - always succeeds
        // In Sprint 2, integrate with actual payment provider
        return true;
    }
}
