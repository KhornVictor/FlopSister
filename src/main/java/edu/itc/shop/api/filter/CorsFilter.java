
package edu.itc.shop.api.filter;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.HEADER_DECORATOR - 100)
public class CorsFilter
        implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String ALLOWED_ORIGIN =
            "http://localhost:5173";

    @Override
    public void filter(ContainerRequestContext request)
            throws IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            request.abortWith(
                    jakarta.ws.rs.core.Response.ok().build()
            );
        }
    }

    @Override
    public void filter(
            ContainerRequestContext request,
            ContainerResponseContext response)
            throws IOException {

        response.getHeaders().putSingle(
                "Access-Control-Allow-Origin", ALLOWED_ORIGIN);

        response.getHeaders().putSingle(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, PATCH, DELETE, OPTIONS");

        response.getHeaders().putSingle(
                "Access-Control-Allow-Headers",
                "Content-Type, X-Api-Key");

        response.getHeaders().putSingle(
                "Access-Control-Expose-Headers",
                "X-Correlation-Id");

        response.getHeaders().putSingle(
                "Access-Control-Max-Age", "3600");
    }
}
