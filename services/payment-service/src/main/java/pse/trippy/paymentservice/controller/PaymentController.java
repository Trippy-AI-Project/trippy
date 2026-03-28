package pse.trippy.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pse.trippy.paymentservice.dto.CheckoutRequestDto;
import pse.trippy.paymentservice.dto.CheckoutResponseDto;
import pse.trippy.paymentservice.dto.PlanDto;
import pse.trippy.paymentservice.service.PaymentService;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Controller for payment operations.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Get all available subscription plans.
     *
     * @return list of available plans
     */
    @GetMapping("/plans")
    public ResponseEntity<List<PlanDto>> getPlans() {
        List<PlanDto> plans = paymentService.getAvailablePlans();
        return ResponseEntity.ok(plans);
    }

    /**
     * Process subscription checkout.
     * Requires authenticated user via JWT/Principal.
     *
     * @param request the checkout request
     * @param principal the authenticated user principal
     * @return checkout response with transaction details
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutRequestDto request, Principal principal) {
        try {
            // Validate request
            if (request.getPlanId() == null || request.getPlanId().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.validation("planId is required"));
            }
            if (request.getPaymentMethodId() == null || request.getPaymentMethodId().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ErrorResponse.validation("paymentMethodId is required"));
            }
            UUID userId;
            try {
                userId = UUID.fromString(principal.getName());
            } catch (Exception ex) {
                // Authentication/config error, not a client error
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ErrorResponse.internal("Invalid principal UUID format"));
            }
            CheckoutResponseDto response = paymentService.checkout(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.validation(e.getMessage()));
        } catch (Exception e) {
            // Do not expose internal exception messages
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.internal("Unexpected server error"));
        }
    }

    /**
     * Error response DTO.
     */
    /**
     * Contract-aligned error response DTO.
     */
    public record ErrorResponse(String error, String message, String timestamp) {
        public static ErrorResponse validation(String message) {
            return new ErrorResponse("VALIDATION_ERROR", message, java.time.Instant.now().toString());
        }
        public static ErrorResponse internal(String message) {
            return new ErrorResponse("INTERNAL_ERROR", message, java.time.Instant.now().toString());
        }
    }
}
