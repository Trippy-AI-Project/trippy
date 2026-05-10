package pse.trippy.apigateway.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.password=",
        "trippy.gateway.jwks-uri=http://localhost:8081/.well-known/jwks.json"
})
@DisplayName("Health Endpoint Integration Test")
class HealthEndpointIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("GET /actuator/health returns 200")
    void healthEndpointReturns200() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").exists();
    }

    @Test
    @DisplayName("GET /actuator/health/liveness returns 200")
    void livenessProbeReturns200() {
        webTestClient.get()
                .uri("/actuator/health/liveness")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    @DisplayName("GET /actuator/health/readiness returns 200")
    void readinessProbeReturns200() {
        webTestClient.get()
                .uri("/actuator/health/readiness")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").exists();
    }

    @Test
    @DisplayName("GET /actuator/info returns 200 with app info")
    void infoEndpointReturnsAppInfo() {
        webTestClient.get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.app.name").isEqualTo("Trippy API Gateway");
    }

    @Test
    @DisplayName("GET /health returns aggregated downstream health")
    void customHealthEndpointReturnsAggregated() {
        webTestClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").exists();
    }
}
