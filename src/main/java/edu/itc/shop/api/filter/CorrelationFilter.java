
package edu.itc.shop.api.filter;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CorrelationFilter
        implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER =
            Logger.getLogger(CorrelationFilter.class.getName());

    private static final String HEADER = "X-Correlation-Id";
    private static final String PROPERTY = "correlationId";

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String correlationId = UUID.randomUUID().toString();

        request.setProperty(PROPERTY, correlationId);
        request.getHeaders().putSingle(HEADER, correlationId);
    }

    @Override
    public void filter(
            ContainerRequestContext request,
            ContainerResponseContext response) throws IOException {

        String correlationId =
                (String) request.getProperty(PROPERTY);

        response.getHeaders().putSingle(HEADER, correlationId);

        LOGGER.info(() ->
                correlationId + " "
                + request.getMethod() + " "
                + request.getUriInfo().getRequestUri()
                + " -> " + response.getStatus());
    }
}
