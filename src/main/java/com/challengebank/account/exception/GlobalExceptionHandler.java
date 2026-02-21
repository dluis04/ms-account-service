package com.challengebank.account.exception;

import com.challengebank.account.model.dto.response.ErrorResponse;
import com.challengebank.account.model.dto.response.FieldError;
import io.quarkus.logging.Log;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.RestResponse;

import java.time.LocalDateTime;
import java.util.List;

public class GlobalExceptionHandler {

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex, UriInfo uriInfo) {
        ErrorResponse error = buildError(404, "Not Found", ex.getMessage(), uriInfo);
        return RestResponse.status(Response.Status.NOT_FOUND, error);
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex, UriInfo uriInfo) {
        ErrorResponse error = buildError(400, "Bad Request", ex.getMessage(), uriInfo);
        return RestResponse.status(Response.Status.BAD_REQUEST, error);
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleInvalidAccountOperation(InvalidAccountOperationException ex, UriInfo uriInfo) {
        ErrorResponse error = buildError(400, "Bad Request", ex.getMessage(), uriInfo);
        return RestResponse.status(Response.Status.BAD_REQUEST, error);
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleReservationNotFound(ReservationNotFoundException ex, UriInfo uriInfo) {
        ErrorResponse error = buildError(404, "Not Found", ex.getMessage(), uriInfo);
        return RestResponse.status(Response.Status.NOT_FOUND, error);
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, UriInfo uriInfo) {
        List<FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> {
                    String path = v.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return new FieldError(field, v.getMessage());
                })
                .toList();

        ErrorResponse error = buildError(400, "Bad Request", "Validation failed", uriInfo);
        error.errors = fieldErrors;
        return RestResponse.status(Response.Status.BAD_REQUEST, error);
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, UriInfo uriInfo) {
        ErrorResponse error = buildError(400, "Bad Request", ex.getMessage(), uriInfo);
        return RestResponse.status(Response.Status.BAD_REQUEST, error);
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleGenericException(Exception ex, UriInfo uriInfo) {
        Log.error("Unhandled exception", ex);
        ErrorResponse error = buildError(500, "Internal Server Error", "An unexpected error occurred", uriInfo);
        return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR, error);
    }

    private ErrorResponse buildError(int status, String error, String message, UriInfo uriInfo) {
        ErrorResponse resp = new ErrorResponse();
        resp.timestamp = LocalDateTime.now();
        resp.status = status;
        resp.error = error;
        resp.message = message;
        resp.path = uriInfo != null ? uriInfo.getPath() : "";
        return resp;
    }
}
