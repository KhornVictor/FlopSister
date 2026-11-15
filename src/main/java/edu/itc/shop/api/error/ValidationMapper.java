package edu.itc.shop.api.error;

import java.util.Comparator;
import java.util.Map;

import edu.itc.shop.api.dto.Problem;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ValidationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        var errors = exception.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", lastNode(violation.getPropertyPath()),
                        "message", violation.getMessage()))
                .sorted(Comparator.comparing(error -> error.get("field")))
                .toList();
        var problem = new Problem(
                "about:blank",
                "Invalid request",
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Validation failed",
                uriInfo.getRequestUri().getPath(),
                errors);
        return Response.status(Response.Status.BAD_REQUEST)
                .type(Problem.MEDIA_TYPE)
                .entity(problem)
                .build();
    }

    private static String lastNode(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name == null ? "request" : name;
    }
}
