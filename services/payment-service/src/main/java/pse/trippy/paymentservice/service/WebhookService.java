package pse.trippy.paymentservice.service;

import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pse.trippy.paymentservice.config.RabbitMQConfig;
import pse.trippy.paymentservice.model.entity.Subscription;
import pse.trippy.paymentservice.model.entity.Transaction;
import pse.trippy.paymentservice.model.entity.WebhookEvent;
import pse.trippy.paymentservice.model.enums.SubscriptionPlan;
import pse.trippy.paymentservice.model.enums.SubscriptionStatus;
import pse.trippy.paymentservice.model.enums.TransactionStatus;
import pse.trippy.paymentservice.model.enums.TransactionType;
import pse.trippy.paymentservice.repository.SubscriptionRepository;
import pse.trippy.paymentservice.repository.TransactionRepository;
import pse.trippy.paymentservice.repository.WebhookEventRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void handleCheckoutSessionCompleted(Session session) {
        String sessionId = session.getId();

        WebhookEvent webhookEvent = WebhookEvent.builder()
                .checkoutSessionId(sessionId)
                .eventType("checkout.session.completed")
                .processedAt(Instant.now())
                .build();

        try {
            webhookEventRepository.saveAndFlush(webhookEvent);
        } catch (DataIntegrityViolationException e) {
            log.info("Webhook event already processed for session: {}", sessionId);
            return;
        }

        // Extract data from session
        String clientReferenceId = session.getClientReferenceId();
        UUID userId = parseUserId(clientReferenceId);
        String customerEmail = session.getCustomerEmail();

        Map<String, String> metadata = session.getMetadata();
        String planId = metadata != null ? metadata.get("planId") : "premium_monthly";
        SubscriptionPlan plan = resolvePlan(planId);
        boolean isYearly = planId != null && planId.endsWith("_yearly");

        Long amountTotal = session.getAmountTotal();
        BigDecimal price = amountTotal != null
                ? BigDecimal.valueOf(amountTotal).divide(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
            String currency = plan.getCurrency();

        // Create or update subscription
        LocalDate now = LocalDate.now();
        LocalDate periodEnd = isYearly ? now.plusYears(1) : now.plusMonths(1);

        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .map(existing -> {
                    existing.setPlan(plan);
                    existing.setStatus(SubscriptionStatus.ACTIVE);
                    existing.setCurrentPeriodStart(now);
                    existing.setCurrentPeriodEnd(periodEnd);
                    existing.setCancelAtPeriodEnd(false);
                    existing.setPriceAmount(price);
                    existing.setCurrency(currency);
                    return existing;
                })
                .orElseGet(() -> Subscription.builder()
                        .userId(userId)
                        .plan(plan)
                        .status(SubscriptionStatus.ACTIVE)
                        .currentPeriodStart(now)
                        .currentPeriodEnd(periodEnd)
                        .priceAmount(price)
                        .currency(currency)
                        .build());
        subscription = subscriptionRepository.save(subscription);

        transactionRepository.save(Transaction.builder()
                .userId(userId)
                .planId(plan == SubscriptionPlan.ENTERPRISE ? pse.trippy.paymentservice.model.enums.PlanType.ENTERPRISE : pse.trippy.paymentservice.model.enums.PlanType.PREMIUM)
                .amount(price)
                .currency(currency)
                .status(TransactionStatus.COMPLETED)
                .type(TransactionType.SUBSCRIPTION)
                .description(customerEmail != null && !customerEmail.isBlank()
                        ? "Stripe checkout for " + customerEmail
                        : "Stripe checkout session " + sessionId)
                .build());

        log.info("Subscription activated via webhook for user {} ({}) - plan: {}, session: {}",
                userId, customerEmail, plan, sessionId);

        publishSubscriptionActivatedEvent(userId, subscription);
    }

    private UUID parseUserId(String clientReferenceId) {
        if (clientReferenceId == null || clientReferenceId.isBlank()) {
            throw new IllegalArgumentException("Missing client reference id");
        }

        try {
            return UUID.fromString(clientReferenceId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid client reference id: " + clientReferenceId, e);
        }
    }

    private SubscriptionPlan resolvePlan(String planId) {
        if (planId != null && planId.startsWith("enterprise")) {
            return SubscriptionPlan.ENTERPRISE;
        }
        return SubscriptionPlan.PREMIUM;
    }

    private void publishSubscriptionActivatedEvent(UUID userId, Subscription subscription) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "payment.subscription.activated",
                    "userId", userId.toString(),
                    "plan", subscription.getPlan().name(),
                    "status", subscription.getStatus().name(),
                    "periodStart", subscription.getCurrentPeriodStart().toString(),
                    "periodEnd", subscription.getCurrentPeriodEnd().toString(),
                    "timestamp", Instant.now().toString()
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.PAYMENT_EXCHANGE,
                    "payment.subscription.activated",
                    event);
            log.info("Published payment.subscription.activated event for user {}", userId);
        } catch (Exception e) {
            log.warn("Failed to publish subscription activated event for user {}: {}", userId, e.getMessage());
        }
    }
}
