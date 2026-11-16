package edu.itc.shop.api.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;

import edu.itc.shop.api.dto.Problem;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * Enforces {@code X-Api-Key} authentication before resource matching on state-changing HTTP methods.
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyFilter implements ContainerRequestFilter {

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String API_KEYS_PROPERTY = "shop.api.keys";
    private static final String DEFAULT_API_KEY = "demo-123";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    @Override
    public void filter(ContainerRequestContext request) {
        if (SAFE_METHODS.contains(request.getMethod().toUpperCase())) {
            return;
        }

        String providedKey = request.getHeaderString(API_KEY_HEADER);
        if (providedKey != null && isValidKey(providedKey)) {
            return;
        }

        Problem problem = new Problem(
                "about:blank",
                "Unauthorized",
                Response.Status.UNAUTHORIZED.getStatusCode(),
                "Missing or invalid X-Api-Key",
                request.getUriInfo().getRequestUri().getPath(),
                null);
        request.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .header("WWW-Authenticate", "ApiKey realm=\"shop\"")
                .type(Problem.MEDIA_TYPE)
                .entity(problem)
                .build());
    }

    private static boolean isValidKey(String providedKey) {
        byte[] providedBytes = providedKey.getBytes(StandardCharsets.UTF_8);
        return Arrays.stream(System.getProperty(API_KEYS_PROPERTY, DEFAULT_API_KEY).split(","))
                .map(String::trim)
                .filter(candidate -> !candidate.isEmpty())
                .anyMatch(candidate -> MessageDigest.isEqual(
                        candidate.getBytes(StandardCharsets.UTF_8),
                        providedBytes));
    }
}
