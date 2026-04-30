package pse.trippy.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.paymentservice.dto.request.CheckoutRequest;
import pse.trippy.paymentservice.dto.response.CheckoutResponse;
import pse.trippy.paymentservice.dto.response.PlanResponse;
import pse.trippy.paymentservice.exception.InvalidPlanException;
import pse.trippy.paymentservice.model.entity.Transaction;
import pse.trippy.paymentservice.model.enums.PlanType;
import pse.trippy.paymentservice.model.enums.TransactionStatus;
import pse.trippy.paymentservice.repository.TransactionRepository;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;

    public List<PlanResponse> getAvailablePlans() {
        return Arrays.stream(PlanType.values())
                .map(plan -> PlanResponse.builder()
                        .planId(plan.name())
                        .displayName(plan.getDisplayName())
                        .price(plan.getPrice())
                        .currency(plan.getCurrency())
                        .features(plan.getFeatures())
                        .build())
                .toList();
    }

    @Transactional
    public CheckoutResponse checkout(UUID userId, CheckoutRequest request) {
        PlanType plan = parsePlan(request.getPlanId());

        // Dummy payment verification — always succeeds for now
        log.info("Processing checkout for user {} on plan {} with paymentMethod {}",
                userId, plan.name(), request.getPaymentMethodId());

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .userId(userId)
                .planId(plan)
                .amount(plan.getPrice())
                .currency(plan.getCurrency())
                .status(TransactionStatus.COMPLETED)
                .build());

        log.info("Transaction {} completed for user {}", transaction.getId(), userId);

        // TODO (Sprint 2): publish event to user-service to update subscription

        return CheckoutResponse.builder()
                .transactionId(transaction.getId())
                .status(transaction.getStatus().name())
                .plan(plan.name())
                .amount(CheckoutResponse.Amount.builder()
                        .value(plan.getPrice())
                        .currency(plan.getCurrency())
                        .build())
                .message("Subscription activated successfully")
                .build();
    }

    private PlanType parsePlan(String planId) {
        try {
            return PlanType.valueOf(planId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidPlanException(planId);
        }
    }
}
