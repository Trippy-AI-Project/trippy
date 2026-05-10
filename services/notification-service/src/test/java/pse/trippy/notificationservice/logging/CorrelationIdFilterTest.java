package pse.trippy.notificationservice.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void doFilter_usesIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIds.HEADER_NAME, "request-123");
        AtomicReference<String> correlationIdInChain = new AtomicReference<>();

        FilterChain chain = (servletRequest, servletResponse) ->
                correlationIdInChain.set(MDC.get(CorrelationIds.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIds.HEADER_NAME)).isEqualTo("request-123");
        assertThat(correlationIdInChain.get()).isEqualTo("request-123");
        assertThat(MDC.get(CorrelationIds.MDC_KEY)).isNull();
    }

    @Test
    void doFilter_replacesInvalidCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/notifications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(CorrelationIds.HEADER_NAME, "bad value with spaces");
        AtomicReference<String> correlationIdInChain = new AtomicReference<>();

        FilterChain chain = (servletRequest, servletResponse) ->
                correlationIdInChain.set(MDC.get(CorrelationIds.MDC_KEY));

        filter.doFilter(request, response, chain);

        String generatedCorrelationId = response.getHeader(CorrelationIds.HEADER_NAME);
        assertThat(generatedCorrelationId).isNotEqualTo("bad value with spaces");
        assertThat(UUID.fromString(generatedCorrelationId)).isNotNull();
        assertThat(correlationIdInChain.get()).isEqualTo(generatedCorrelationId);
        assertThat(MDC.get(CorrelationIds.MDC_KEY)).isNull();
    }
}
