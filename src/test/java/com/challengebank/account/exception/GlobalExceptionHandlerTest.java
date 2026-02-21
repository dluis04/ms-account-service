package com.challengebank.account.exception;

import com.challengebank.account.model.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    UriInfo uriInfo;

    @InjectMocks
    GlobalExceptionHandler handler;

    @Test
    void testHandleAccountNotFound() {
        when(uriInfo.getPath()).thenReturn("/accounts/1234567890");
        AccountNotFoundException ex = new AccountNotFoundException("Account not found: 1234567890");

        RestResponse<ErrorResponse> response = handler.handleAccountNotFound(ex, uriInfo);

        assertEquals(404, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(404, body.status);
        assertEquals("Not Found", body.error);
        assertEquals("Account not found: 1234567890", body.message);
        assertEquals("/accounts/1234567890", body.path);
        assertNotNull(body.timestamp);
    }

    @Test
    void testHandleInsufficientFunds() {
        when(uriInfo.getPath()).thenReturn("/accounts/1234567890/balance/update");
        InsufficientFundsException ex = new InsufficientFundsException("Insufficient funds for withdrawal of 5000");

        RestResponse<ErrorResponse> response = handler.handleInsufficientFunds(ex, uriInfo);

        assertEquals(400, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(400, body.status);
        assertEquals("Bad Request", body.error);
        assertEquals("Insufficient funds for withdrawal of 5000", body.message);
        assertEquals("/accounts/1234567890/balance/update", body.path);
        assertNotNull(body.timestamp);
    }

    @Test
    void testHandleInvalidAccountOperation() {
        when(uriInfo.getPath()).thenReturn("/accounts/1234567890");
        InvalidAccountOperationException ex = new InvalidAccountOperationException("Cannot close account with non-zero balance");

        RestResponse<ErrorResponse> response = handler.handleInvalidAccountOperation(ex, uriInfo);

        assertEquals(400, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(400, body.status);
        assertEquals("Bad Request", body.error);
        assertEquals("Cannot close account with non-zero balance", body.message);
        assertNotNull(body.timestamp);
    }

    @Test
    void testHandleReservationNotFound() {
        when(uriInfo.getPath()).thenReturn("/accounts/1234567890/reserve/abc");
        ReservationNotFoundException ex = new ReservationNotFoundException("Reservation not found");

        RestResponse<ErrorResponse> response = handler.handleReservationNotFound(ex, uriInfo);

        assertEquals(404, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(404, body.status);
        assertEquals("Not Found", body.error);
        assertEquals("Reservation not found", body.message);
        assertNotNull(body.timestamp);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testHandleConstraintViolation() {
        when(uriInfo.getPath()).thenReturn("/accounts");

        ConstraintViolation<Object> violation1 = mock(ConstraintViolation.class);
        Path path1 = mock(Path.class);
        when(path1.toString()).thenReturn("createAccount.request.customerId");
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(violation1.getMessage()).thenReturn("must not be null");

        ConstraintViolation<Object> violation2 = mock(ConstraintViolation.class);
        Path path2 = mock(Path.class);
        when(path2.toString()).thenReturn("amount");
        when(violation2.getPropertyPath()).thenReturn(path2);
        when(violation2.getMessage()).thenReturn("must be greater than 0");

        Set<ConstraintViolation<?>> violations = Set.of(violation1, violation2);
        ConstraintViolationException ex = new ConstraintViolationException("Validation failed", violations);

        RestResponse<ErrorResponse> response = handler.handleConstraintViolation(ex, uriInfo);

        assertEquals(400, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(400, body.status);
        assertEquals("Bad Request", body.error);
        assertEquals("Validation failed", body.message);
        assertEquals("/accounts", body.path);
        assertNotNull(body.errors);
        assertEquals(2, body.errors.size());

        boolean hasCustomerId = body.errors.stream()
                .anyMatch(fe -> "customerId".equals(fe.field) && "must not be null".equals(fe.message));
        boolean hasAmount = body.errors.stream()
                .anyMatch(fe -> "amount".equals(fe.field) && "must be greater than 0".equals(fe.message));
        assertTrue(hasCustomerId, "Should contain customerId field error");
        assertTrue(hasAmount, "Should contain amount field error");
    }

    @Test
    void testHandleIllegalArgument() {
        when(uriInfo.getPath()).thenReturn("/accounts/validate/funds");
        IllegalArgumentException ex = new IllegalArgumentException("Invalid account number format");

        RestResponse<ErrorResponse> response = handler.handleIllegalArgument(ex, uriInfo);

        assertEquals(400, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(400, body.status);
        assertEquals("Bad Request", body.error);
        assertEquals("Invalid account number format", body.message);
        assertNotNull(body.timestamp);
    }

    @Test
    void testHandleGenericException() {
        when(uriInfo.getPath()).thenReturn("/accounts");
        Exception ex = new RuntimeException("Something unexpected");

        RestResponse<ErrorResponse> response = handler.handleGenericException(ex, uriInfo);

        assertEquals(500, response.getStatus());
        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals(500, body.status);
        assertEquals("Internal Server Error", body.error);
        assertEquals("An unexpected error occurred", body.message);
        assertEquals("/accounts", body.path);
        assertNotNull(body.timestamp);
    }

    @Test
    void testBuildError_nullUriInfo() {
        AccountNotFoundException ex = new AccountNotFoundException("Not found");

        RestResponse<ErrorResponse> response = handler.handleAccountNotFound(ex, null);

        ErrorResponse body = response.getEntity();
        assertNotNull(body);
        assertEquals("", body.path);
    }
}
