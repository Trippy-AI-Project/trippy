package pse.trippy.apigateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * Controller for API Gateway health checks and status endpoints.
 */
@RestController
public class HealthController {

    /**
     * Health check endpoint.
     * Returns 200 OK with status UP.
     *
     * @return response with status UP
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Collections.singletonMap("status", "UP"));
    }
}
