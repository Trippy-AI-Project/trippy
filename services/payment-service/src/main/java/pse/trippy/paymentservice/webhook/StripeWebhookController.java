package pse.trippy.paymentservice.webhook;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
pse.trippy.paymentservice.service.SubscriptionService;

@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
public class StripeWebhookController {
    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final SubscriptionService subscriptionService;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    @PostMapping
    public ResponseEntity<String> handleStripeEvent(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.warn("⚠️  Webhook signature verification failed.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            // TODO: Implement logic to update subscription and publish event
            log.info("Received checkout.session.completed event: {}", event.getId());
            // subscriptionService.handleCheckoutSessionCompleted(event);
        }
        // Optionally handle other event types
        return ResponseEntity.ok("Received");
    }
}
