package pse.trippy.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter to inject X-Correlation-ID header to all requests.
 * If the header is already present, it will be preserved.
 * If not present, a new UUID will be generated and added to the request.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Check if correlation ID header already exists
        if (!exchange.getRequest().getHeaders().containsKey(CORRELATION_ID_HEADER)) {
            // Generate new correlation ID if not present
            String correlationId = UUID.randomUUID().toString();
            
            // Add to request headers
            exchange = exchange.mutate()
                    .request(exchange.getRequest().mutate()
                            .header(CORRELATION_ID_HEADER, correlationId)
                            .build())
                    .build();
        }

        // Add correlation ID to response headers as well
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId != null) {
            exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        }

        return chain.filter(exchange);
    }
}
