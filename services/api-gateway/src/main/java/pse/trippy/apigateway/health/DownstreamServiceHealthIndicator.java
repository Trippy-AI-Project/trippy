package pse.trippy.apigateway.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator that checks all downstream services.
 */
@Component
@ConfigurationProperties(prefix = "trippy.gateway")
public class DownstreamServiceHealthIndicator implements ReactiveHealthIndicator {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final WebClient.Builder webClientBuilder;
    private Map<String, String> downstreamServices = new HashMap<>();

    public DownstreamServiceHealthIndicator(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Map<String, String> getDownstreamServices() {
        return downstreamServices;
    }

    public void setDownstreamServices(Map<String, String> downstreamServices) {
        this.downstreamServices = downstreamServices;
    }

    @Override
    public Mono<Health> health() {
        if (downstreamServices.isEmpty()) {
            return Mono.just(Health.up().build());
        }

        return Flux.fromIterable(downstreamServices.entrySet())
                .flatMap(entry -> checkService(entry.getKey(), entry.getValue()))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .map(statuses -> {
                    Health.Builder builder = statuses.containsValue("DOWN")
                            ? Health.down() : Health.up();
                    statuses.forEach(builder::withDetail);
                    return builder.build();
                });
    }

    private Mono<Map.Entry<String, String>> checkService(String name, String baseUrl) {
        return webClientBuilder.build()
                .get()
                .uri(baseUrl + "/actuator/health")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TIMEOUT)
                .map(response -> {
                    String status = response.containsKey("status")
                            ? response.get("status").toString() : "UP";
                    return Map.entry(name, status);
                })
                .onErrorResume(ex -> Mono.just(Map.entry(name, "DOWN")));
    }
}
