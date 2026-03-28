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
                        .body(new ErrorResponse("Invalid request: planId is required"));
            }

            if (request.getPaymentMethodId() == null || request.getPaymentMethodId().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid request: paymentMethodId is required"));
            }

            // Get user ID from principal (in real scenario, extract from JWT token)
            // For now, use a dummy UUID from principal name
            UUID userId = UUID.fromString(principal.getName());

            // Process checkout
            CheckoutResponseDto response = paymentService.checkout(userId, request);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Bad request: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Error response DTO.
     */
    public record ErrorResponse(String error) {}
}
