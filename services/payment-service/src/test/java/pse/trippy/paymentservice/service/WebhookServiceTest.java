package pse.trippy.paymentservice.service;

import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import pse.trippy.paymentservice.model.entity.Subscription;
import pse.trippy.paymentservice.model.entity.WebhookEvent;
import pse.trippy.paymentservice.model.enums.SubscriptionPlan;
import pse.trippy.paymentservice.model.enums.SubscriptionStatus;
import pse.trippy.paymentservice.repository.SubscriptionRepository;
import pse.trippy.paymentservice.repository.WebhookEventRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookService")
class WebhookServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private WebhookEventRepository webhookEventRepository;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private WebhookService webhookService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    @DisplayName("handles checkout.session.completed and creates subscription")
    void handlesCheckoutSessionCompletedCreatesSubscription() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_123");
        when(session.getClientReferenceId()).thenReturn(USER_ID.toString());
        when(session.getCustomerEmail()).thenReturn("user@test.com");
        when(session.getAmountTotal()).thenReturn(999L);
        when(session.getMetadata()).thenReturn(Map.of("planId", "premium_monthly"));

        when(webhookEventRepository.existsByCheckoutSessionId("cs_test_123")).thenReturn(false);
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> {
            Subscription s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        webhookService.handleCheckoutSessionCompleted(session);

        verify(webhookEventRepository).save(any(WebhookEvent.class));
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(rabbitTemplate).convertAndSend(eq("payment.events"), eq("payment.subscription.activated"), any(Map.class));
    }

    @Test
    @DisplayName("skips duplicate webhook (idempotent via session ID)")
    void skipsDuplicateWebhook() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_duplicate");
        when(webhookEventRepository.existsByCheckoutSessionId("cs_test_duplicate")).thenReturn(true);

        webhookService.handleCheckoutSessionCompleted(session);

        verify(subscriptionRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("updates existing subscription on webhook for enterprise plan")
    void updatesExistingSubscriptionOnWebhook() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("cs_test_456");
        when(session.getClientReferenceId()).thenReturn(USER_ID.toString());
        when(session.getCustomerEmail()).thenReturn("user@test.com");
        when(session.getAmountTotal()).thenReturn(29999L);
        when(session.getMetadata()).thenReturn(Map.of("planId", "enterprise_monthly"));

        when(webhookEventRepository.existsByCheckoutSessionId("cs_test_456")).thenReturn(false);
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(i -> i.getArgument(0));

        Subscription existing = Subscription.builder()
                .userId(USER_ID)
                .plan(SubscriptionPlan.PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(LocalDate.now().minusMonths(1))
                .currentPeriodEnd(LocalDate.now())
                .priceAmount(new BigDecimal("9.99"))
                .build();
        existing.setId(UUID.randomUUID());
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(i -> i.getArgument(0));

        webhookService.handleCheckoutSessionCompleted(session);

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(rabbitTemplate).convertAndSend(eq("payment.events"), eq("payment.subscription.activated"), any(Map.class));
    }
}
