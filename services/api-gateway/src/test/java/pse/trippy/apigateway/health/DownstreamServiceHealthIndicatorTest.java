package pse.trippy.apigateway.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DownstreamServiceHealthIndicatorTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    @SuppressWarnings("rawtypes")
    private RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private ResponseSpec responseSpec;

    private DownstreamServiceHealthIndicator indicator;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        lenient().when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        indicator = new DownstreamServiceHealthIndicator(webClientBuilder);
    }

    @Test
    void healthReturnsUpWhenAllServicesAreUp() {
        Map<String, Object> upResponse = Map.of("status", "UP");
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(upResponse));

        Map<String, String> services = new HashMap<>();
        services.put("user-service", "http://user-service:8081");
        services.put("trip-service", "http://trip-service:8082");
        indicator.setDownstreamServices(services);

        StepVerifier.create(indicator.health())
                .assertNext(health -> {
                    assertThat(health.getStatus()).isEqualTo(Status.UP);
                    assertThat(health.getDetails()).containsEntry("user-service", "UP");
                    assertThat(health.getDetails()).containsEntry("trip-service", "UP");
                })
                .verifyComplete();
    }

    @Test
    void healthReturnsDownWhenServiceIsDown() {
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        Map<String, String> services = new HashMap<>();
        services.put("user-service", "http://user-service:8081");
        indicator.setDownstreamServices(services);

        StepVerifier.create(indicator.health())
                .assertNext(health -> {
                    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
                    assertThat(health.getDetails()).containsEntry("user-service", "DOWN");
                })
                .verifyComplete();
    }

    @Test
    void healthReturnsUpWhenNoServicesConfigured() {
        indicator = new DownstreamServiceHealthIndicator(webClientBuilder);
        indicator.setDownstreamServices(new HashMap<>());

        StepVerifier.create(indicator.health())
                .assertNext(health -> assertThat(health.getStatus()).isEqualTo(Status.UP))
                .verifyComplete();
    }

    @Test
    void healthReturnsDownWhenAtLeastOneServiceIsDown() {
        Map<String, Object> upResponse = Map.of("status", "UP");

        // First call succeeds, second fails
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(upResponse))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        Map<String, String> services = new HashMap<>();
        services.put("user-service", "http://user-service:8081");
        services.put("trip-service", "http://trip-service:8082");
        indicator.setDownstreamServices(services);

        StepVerifier.create(indicator.health())
                .assertNext(health -> {
                    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
                    // At least one service should be DOWN
                    assertThat(health.getDetails().values()).contains("DOWN");
                })
                .verifyComplete();
    }
}
