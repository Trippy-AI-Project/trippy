package pse.trippy.notificationservice.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        String correlationId = CorrelationIds.resolve(request.getHeader(CorrelationIds.HEADER_NAME));
        MDC.put(CorrelationIds.MDC_KEY, correlationId);
        response.setHeader(CorrelationIds.HEADER_NAME, correlationId);

        try {
            filterChain.doFilter(request, response);
            log.info("HTTP request completed method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    LogSanitizer.safeDetail(request.getRequestURI()),
                    response.getStatus(),
                    elapsedMs(startedAt));
        } catch (IOException ex) {
            log.warn("HTTP request failed method={} path={} durationMs={} error={}",
                    request.getMethod(),
                    LogSanitizer.safeDetail(request.getRequestURI()),
                    elapsedMs(startedAt),
                    LogSanitizer.safeError(ex));
            throw ex;
        } catch (ServletException ex) {
            log.warn("HTTP request failed method={} path={} durationMs={} error={}",
                    request.getMethod(),
                    LogSanitizer.safeDetail(request.getRequestURI()),
                    elapsedMs(startedAt),
                    LogSanitizer.safeError(ex));
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("HTTP request failed method={} path={} durationMs={} error={}",
                    request.getMethod(),
                    LogSanitizer.safeDetail(request.getRequestURI()),
                    elapsedMs(startedAt),
                    LogSanitizer.safeError(ex));
            throw ex;
        } finally {
            MDC.remove(CorrelationIds.MDC_KEY);
        }
    }

    private long elapsedMs(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }
}
