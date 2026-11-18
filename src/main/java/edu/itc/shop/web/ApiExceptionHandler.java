package edu.itc.shop.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import edu.itc.shop.service.ProductNotFoundException;
import edu.itc.shop.service.UsernameTakenException;
import jakarta.servlet.http.HttpServletRequest;


/**
 * Translates application exceptions into RFC 9457 ProblemDetail responses.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Individual bean-validation failure item exposed in ProblemDetail responses.
     */
    public record FieldValidationError(String field, String message) {
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(
            ProductNotFoundException ex,
            HttpServletRequest request) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage(),
                request
        );
    }

    /**
     * Handles attempts to register an already-used username.
     */
    @ExceptionHandler(UsernameTakenException.class)
    public ProblemDetail handleUsernameTaken(
            UsernameTakenException ex,
            HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "Conflict",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Not Found",
                "Resource not found",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<FieldValidationError> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> new FieldValidationError(
                        err.getField(),
                        err.getDefaultMessage()
                ))
                .toList();

        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Validation failed",
                request
        );
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ProblemDetail handleBadRequest(
            Exception ex,
            HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Malformed request",
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return problem(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return problem(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Media Type",
                ex.getMessage(),
                request
        );
    }


    /**
     * Handles access-denied exceptions from method-level security.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return problem(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "Access denied",
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(
            AuthenticationException ex,
            HttpServletRequest request) {
        return problem(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Bad credentials",
                request
        );
    }

    private static ProblemDetail problem(
            HttpStatus status,
            String title,
            String message,
            HttpServletRequest request) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                status,
                message
        );
        detail.setType(URI.create("about:blank"));
        detail.setTitle(title);
        detail.setInstance(URI.create(request.getRequestURI()));
        return detail;
    }
}
