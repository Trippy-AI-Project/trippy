package pse.trippy.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pse.trippy.apigateway.health.DownstreamServiceHealthIndicator;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for API Gateway health checks and status endpoints.
 */
@RestController
public class HealthController {

    private final DownstreamServiceHealthIndicator downstreamServiceHealthIndicator;

    public HealthController(DownstreamServiceHealthIndicator downstreamServiceHealthIndicator) {
        this.downstreamServiceHealthIndicator = downstreamServiceHealthIndicator;
    }

    /**
     * Health check endpoint.
     * Returns status UP/DOWN with downstream service statuses.
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        return downstreamServiceHealthIndicator.health().map(health -> {
            Map<String, Object> response = new HashMap<>();
            response.put("status", health.getStatus().getCode());
            response.put("services", health.getDetails());
            return response;
        });
    }
}
