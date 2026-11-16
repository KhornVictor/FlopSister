
package edu.itc.shop.api.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    private static final String API_KEY_HEADER = "X-Api-Key";

    // Development key for this lab
    private static final String EXPECTED_API_KEY = "demo-123";

    @Override
    public void filter(ContainerRequestContext request)
            throws IOException {

        String method = request.getMethod();

        // Public requests do not require an API key
        if ("GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }

        String providedKey = request.getHeaderString(API_KEY_HEADER);

        if (providedKey == null || !MessageDigest.isEqual(
                EXPECTED_API_KEY.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8))) {

            request.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .type(MediaType.valueOf("application/problem+json"))
                            .entity("{\"type\":\"about:blank\","
                                    + "\"title\":\"Unauthorized\","
                                    + "\"status\":401,"
                                    + "\"detail\":\"Missing or invalid API key\"}")
                            .build()
            );
        }
    }
}
