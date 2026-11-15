package edu.itc.shop.api.error;

import java.util.UUID;

import edu.itc.shop.api.dto.Problem;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ProblemMapper implements ExceptionMapper<Throwable> {

    private static final System.Logger LOGGER = System.getLogger("api");

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException webException
                && webException.getResponse().getStatus()
                        != Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            return webApplicationProblem(webException);
        }

        String reference = UUID.randomUUID().toString();
        LOGGER.log(System.Logger.Level.ERROR,
                "Unhandled exception, reference " + reference,
                exception);
        var problem = new Problem(
                "about:blank",
                "Internal Server Error",
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "reference " + reference,
                uriInfo.getRequestUri().getPath(),
                null);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(Problem.MEDIA_TYPE)
                .entity(problem)
                .build();
    }

    private Response webApplicationProblem(WebApplicationException exception) {
        Response original = exception.getResponse();
        int status = original.getStatus();
        String title = original.getStatusInfo().getReasonPhrase();
        String detail = exception.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = title;
        }

        var problem = new Problem(
                "about:blank",
                title,
                status,
                detail,
                uriInfo.getRequestUri().getPath(),
                null);
        Response.ResponseBuilder response = Response.status(original.getStatusInfo())
                .type(Problem.MEDIA_TYPE)
                .entity(problem);
        original.getHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("Content-Type")
                    && !name.equalsIgnoreCase("Content-Length")) {
                values.forEach(value -> response.header(name, value));
            }
        });
        return response.build();
    }
}
