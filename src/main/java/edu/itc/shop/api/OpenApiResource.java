package edu.itc.shop.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * Serves the build-time generated OpenAPI 3.1 YAML specification at {@code GET /openapi}.
 */
@Path("/openapi")
@Tag(name = "OpenAPI", description = "OpenAPI 3.1 specification endpoint")
public class OpenApiResource {

    private static final String MEDIA_TYPE_YAML = "application/yaml";
    private static final String SPEC_RESOURCE = "/META-INF/openapi.yaml";

    @GET
    @Produces(MEDIA_TYPE_YAML)
    @Operation(summary = "Get OpenAPI specification", description = "Serves the generated openapi.yaml document.")
    @APIResponse(responseCode = "200", description = "OpenAPI YAML document")
    @APIResponse(responseCode = "404", description = "Generated OpenAPI document not found on classpath")
    public String getOpenApi() throws IOException {
        try (InputStream stream = OpenApiResource.class.getResourceAsStream(SPEC_RESOURCE)) {
            if (stream == null) {
                throw new NotFoundException("Generated OpenAPI specification not found");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
