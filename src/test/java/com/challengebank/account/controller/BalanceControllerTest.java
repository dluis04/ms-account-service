package com.challengebank.account.controller;

import com.challengebank.account.exception.AccountNotFoundException;
import com.challengebank.account.exception.InsufficientFundsException;
import com.challengebank.account.exception.InvalidAccountOperationException;
import com.challengebank.account.exception.ReservationNotFoundException;
import com.challengebank.account.model.dto.request.ReserveFundsRequest;
import com.challengebank.account.model.dto.request.UpdateBalanceRequest;
import com.challengebank.account.model.dto.response.BalanceResponse;
import com.challengebank.account.model.dto.response.ReservationResponse;
import com.challengebank.account.model.enums.BalanceOperation;
import com.challengebank.account.model.enums.ReservationStatus;
import com.challengebank.account.service.BalanceService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
class BalanceControllerTest {

    @InjectMock
    BalanceService balanceService;

    private static final String ACCOUNT_NUMBER = "1234567890123456";
    private static final UUID RESERVATION_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    private BalanceResponse buildBalanceResponse() {
        BalanceResponse response = new BalanceResponse();
        response.accountNumber = ACCOUNT_NUMBER;
        response.balance = 10000.0;
        response.availableBalance = 9500.0;
        response.reservedAmount = 500.0;
        response.currency = "USD";
        response.lastUpdated = LocalDateTime.of(2025, 1, 1, 14, 30, 0);
        return response;
    }

    private UpdateBalanceRequest buildDepositRequest() {
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.amount = 500.0;
        request.operation = BalanceOperation.DEPOSIT;
        request.description = "ATM deposit";
        request.referenceId = "TXN-12345";
        return request;
    }

    private ReserveFundsRequest buildReserveRequest() {
        ReserveFundsRequest request = new ReserveFundsRequest();
        request.amount = 500.0;
        request.description = "Pending transfer";
        request.expirationMinutes = 30;
        return request;
    }

    private ReservationResponse buildReservationResponse() {
        ReservationResponse response = new ReservationResponse();
        response.reservationId = RESERVATION_ID;
        response.accountNumber = ACCOUNT_NUMBER;
        response.amount = 500.0;
        response.expiresAt = LocalDateTime.of(2025, 1, 1, 15, 0, 0);
        response.status = ReservationStatus.ACTIVE;
        return response;
    }

    // GET /v1/accounts/{accountNumber}/balance

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void getBalance_success_returnsOk() {
        when(balanceService.getBalance(ACCOUNT_NUMBER)).thenReturn(buildBalanceResponse());

        given()
                .when()
                .get("/v1/accounts/{accountNumber}/balance", ACCOUNT_NUMBER)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("accountNumber", equalTo(ACCOUNT_NUMBER))
                .body("balance", equalTo(10000.0f))
                .body("availableBalance", equalTo(9500.0f))
                .body("reservedAmount", equalTo(500.0f))
                .body("currency", equalTo("USD"));

        verify(balanceService).getBalance(ACCOUNT_NUMBER);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void getBalance_accountNotFound_returns404() {
        String unknownNumber = "9999999999999999";
        when(balanceService.getBalance(unknownNumber))
                .thenThrow(new AccountNotFoundException("Account not found: " + unknownNumber));

        given()
                .when()
                .get("/v1/accounts/{accountNumber}/balance", unknownNumber)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("message", containsString("Account not found"));
    }

    @Test
    void getBalance_unauthorized_returns401() {
        given()
                .when()
                .get("/v1/accounts/{accountNumber}/balance", ACCOUNT_NUMBER)
                .then()
                .statusCode(401);

        verifyNoInteractions(balanceService);
    }

    // POST /v1/accounts/{accountNumber}/balance/update

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateBalance_deposit_returnsOk() {
        when(balanceService.updateBalance(eq(ACCOUNT_NUMBER), any(UpdateBalanceRequest.class)))
                .thenReturn(buildBalanceResponse());

        given()
                .contentType(ContentType.JSON)
                .body(buildDepositRequest())
                .when()
                .post("/v1/accounts/{accountNumber}/balance/update", ACCOUNT_NUMBER)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("accountNumber", equalTo(ACCOUNT_NUMBER));

        verify(balanceService).updateBalance(eq(ACCOUNT_NUMBER), any(UpdateBalanceRequest.class));
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void updateBalance_withRoleUser_returnsForbidden() {
        given()
                .contentType(ContentType.JSON)
                .body(buildDepositRequest())
                .when()
                .post("/v1/accounts/{accountNumber}/balance/update", ACCOUNT_NUMBER)
                .then()
                .statusCode(403);

        verifyNoInteractions(balanceService);
    }

    @Test
    void updateBalance_unauthorized_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(buildDepositRequest())
                .when()
                .post("/v1/accounts/{accountNumber}/balance/update", ACCOUNT_NUMBER)
                .then()
                .statusCode(401);

        verifyNoInteractions(balanceService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateBalance_insufficientFunds_returnsBadRequest() {
        when(balanceService.updateBalance(eq(ACCOUNT_NUMBER), any(UpdateBalanceRequest.class)))
                .thenThrow(new InsufficientFundsException("Insufficient funds for withdrawal"));

        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.amount = 99999.0;
        request.operation = BalanceOperation.WITHDRAWAL;

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/v1/accounts/{accountNumber}/balance/update", ACCOUNT_NUMBER)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("message", containsString("Insufficient funds"));
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateBalance_accountNotActive_returnsBadRequest() {
        when(balanceService.updateBalance(eq(ACCOUNT_NUMBER), any(UpdateBalanceRequest.class)))
                .thenThrow(new InvalidAccountOperationException("Account is not active"));

        given()
                .contentType(ContentType.JSON)
                .body(buildDepositRequest())
                .when()
                .post("/v1/accounts/{accountNumber}/balance/update", ACCOUNT_NUMBER)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("message", containsString("not active"));
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateBalance_validationError_returnsBadRequest() {
        UpdateBalanceRequest invalidRequest = new UpdateBalanceRequest();

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post("/v1/accounts/{accountNumber}/balance/update", ACCOUNT_NUMBER)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"));

        verifyNoInteractions(balanceService);
    }

    // POST /v1/accounts/{accountNumber}/reserve

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void reserveFunds_success_returnsOk() {
        when(balanceService.reserveFunds(eq(ACCOUNT_NUMBER), any(ReserveFundsRequest.class)))
                .thenReturn(buildReservationResponse());

        given()
                .contentType(ContentType.JSON)
                .body(buildReserveRequest())
                .when()
                .post("/v1/accounts/{accountNumber}/reserve", ACCOUNT_NUMBER)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("reservationId", equalTo(RESERVATION_ID.toString()))
                .body("accountNumber", equalTo(ACCOUNT_NUMBER))
                .body("amount", equalTo(500.0f))
                .body("status", equalTo("ACTIVE"));

        verify(balanceService).reserveFunds(eq(ACCOUNT_NUMBER), any(ReserveFundsRequest.class));
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void reserveFunds_insufficientBalance_returnsBadRequest() {
        when(balanceService.reserveFunds(eq(ACCOUNT_NUMBER), any(ReserveFundsRequest.class)))
                .thenThrow(new InsufficientFundsException("Insufficient available balance"));

        given()
                .contentType(ContentType.JSON)
                .body(buildReserveRequest())
                .when()
                .post("/v1/accounts/{accountNumber}/reserve", ACCOUNT_NUMBER)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("message", containsString("Insufficient"));
    }

    @Test
    void reserveFunds_unauthorized_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(buildReserveRequest())
                .when()
                .post("/v1/accounts/{accountNumber}/reserve", ACCOUNT_NUMBER)
                .then()
                .statusCode(401);

        verifyNoInteractions(balanceService);
    }

    // POST /v1/accounts/{accountNumber}/reserve/{reservationId}/release

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void releaseReservedFunds_success_returnsNoContent() {
        doNothing().when(balanceService).releaseReservedFunds(ACCOUNT_NUMBER, RESERVATION_ID);

        given()
                .when()
                .delete("/v1/accounts/{accountNumber}/reservations/{reservationId}",
                        ACCOUNT_NUMBER, RESERVATION_ID)
                .then()
                .statusCode(204);

        verify(balanceService).releaseReservedFunds(ACCOUNT_NUMBER, RESERVATION_ID);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void releaseReservedFunds_reservationNotFound_returns404() {
        UUID unknownReservation = UUID.randomUUID();
        doThrow(new ReservationNotFoundException("Reservation not found: " + unknownReservation))
                .when(balanceService).releaseReservedFunds(ACCOUNT_NUMBER, unknownReservation);

        given()
                .when()
                .delete("/v1/accounts/{accountNumber}/reservations/{reservationId}",
                        ACCOUNT_NUMBER, unknownReservation)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("message", containsString("Reservation not found"));
    }

    @Test
    void releaseReservedFunds_unauthorized_returns401() {
        given()
                .when()
                .delete("/v1/accounts/{accountNumber}/reservations/{reservationId}",
                        ACCOUNT_NUMBER, RESERVATION_ID)
                .then()
                .statusCode(401);

        verifyNoInteractions(balanceService);
    }
}
