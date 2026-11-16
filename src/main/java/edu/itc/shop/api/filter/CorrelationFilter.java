package edu.itc.shop.api.filter;

import java.io.IOException;
import java.util.UUID;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Propagates or generates {@code X-Correlation-Id} and emits an access log entry per request.
 */
@Provider
@Priority(Priorities.USER)
public class CorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final System.Logger LOGGER = System.getLogger(CorrelationFilter.class.getName());
    private static final String HEADER = "X-Correlation-Id";
    private static final String CORRELATION_PROPERTY = "correlationId";
    private static final String START_NANOS_PROPERTY = "requestStartNanos";

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String correlationId = resolveCorrelationId(request);
        request.setProperty(CORRELATION_PROPERTY, correlationId);
        request.setProperty(START_NANOS_PROPERTY, System.nanoTime());
        request.getHeaders().putSingle(HEADER, correlationId);
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response)
            throws IOException {
        Object idProperty = request.getProperty(CORRELATION_PROPERTY);
        String correlationId = idProperty instanceof String existing
                ? existing
                : resolveCorrelationId(request);

        long elapsedMs = 0L;
        Object startProperty = request.getProperty(START_NANOS_PROPERTY);
        if (startProperty instanceof Long startNanos) {
            elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        }

        response.getHeaders().putSingle(HEADER, correlationId);
        LOGGER.log(System.Logger.Level.INFO,
                "{0} {1} -> {2} [{3}] {4} ms",
                request.getMethod(),
                request.getUriInfo().getRequestUri().getPath(),
                response.getStatus(),
                correlationId,
                elapsedMs);
    }

    private static String resolveCorrelationId(ContainerRequestContext request) {
        String incoming = request.getHeaderString(HEADER);
        return (incoming != null && !incoming.isBlank())
                ? incoming.trim()
                : UUID.randomUUID().toString();
    }
}
