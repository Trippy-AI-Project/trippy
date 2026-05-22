package pse.trippy.paymentservice.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.paymentservice.service.WebhookService;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error parsing webhook payload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        log.info("Received Stripe webhook event: type={}, id={}", event.getType(), event.getId());
        return handleWebhookEvent(event);
    }

    ResponseEntity<String> handleWebhookEvent(Event event) {
        switch (event.getType()) {
            case "checkout.session.completed" -> {
                Object dataObject = event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                if (dataObject instanceof Session session) {
                    try {
                        webhookService.handleCheckoutSessionCompleted(session);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid Stripe checkout session payload: {}", e.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
                    }
                } else {
                    log.warn("Stripe webhook event {} did not deserialize a checkout session", event.getId());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
                }
            }
            default -> log.info("Unhandled event type: {}", event.getType());
        }

        return ResponseEntity.ok("Received");
    }
}
