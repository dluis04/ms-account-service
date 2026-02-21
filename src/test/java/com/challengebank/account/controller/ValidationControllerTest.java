package com.challengebank.account.controller;

import com.challengebank.account.model.dto.request.ValidateFundsRequest;
import com.challengebank.account.model.dto.response.AccountValidationResponse;
import com.challengebank.account.model.dto.response.FundsValidationResponse;
import com.challengebank.account.model.enums.AccountStatus;
import com.challengebank.account.service.ValidationService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class ValidationControllerTest {

    @InjectMock
    ValidationService validationService;

    private static final String ACCOUNT_NUMBER = "1234567890123456";

    private ValidateFundsRequest buildValidateFundsRequest() {
        ValidateFundsRequest request = new ValidateFundsRequest();
        request.accountNumber = ACCOUNT_NUMBER;
        request.amount = 1500.0;
        request.includeOverdraft = false;
        return request;
    }

    private FundsValidationResponse buildFundsValidResponse() {
        FundsValidationResponse response = new FundsValidationResponse();
        response.valid = true;
        response.accountNumber = ACCOUNT_NUMBER;
        response.requestedAmount = 1500.0;
        response.availableBalance = 5000.0;
        response.overdraftAvailable = 500.0;
        response.totalAvailable = 5000.0;
        response.message = "Sufficient funds available";
        return response;
    }

    private FundsValidationResponse buildFundsInvalidResponse() {
        FundsValidationResponse response = new FundsValidationResponse();
        response.valid = false;
        response.accountNumber = ACCOUNT_NUMBER;
        response.requestedAmount = 10000.0;
        response.availableBalance = 5000.0;
        response.overdraftAvailable = 500.0;
        response.totalAvailable = 5000.0;
        response.message = "Insufficient funds";
        return response;
    }

    private AccountValidationResponse buildAccountValidResponse() {
        AccountValidationResponse response = new AccountValidationResponse();
        response.valid = true;
        response.accountNumber = ACCOUNT_NUMBER;
        response.exists = true;
        response.status = AccountStatus.ACTIVE;
        response.message = "Account is active and valid";
        return response;
    }

    private AccountValidationResponse buildAccountInvalidResponse() {
        AccountValidationResponse response = new AccountValidationResponse();
        response.valid = false;
        response.accountNumber = ACCOUNT_NUMBER;
        response.exists = true;
        response.status = AccountStatus.BLOCKED;
        response.message = "Account exists but is not active (status: BLOCKED)";
        return response;
    }

    private AccountValidationResponse buildAccountNotFoundResponse() {
        AccountValidationResponse response = new AccountValidationResponse();
        response.valid = false;
        response.accountNumber = "9999999999999999";
        response.exists = false;
        response.message = "Account not found";
        return response;
    }

    // POST /v1/accounts/validate/funds

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateFunds_sufficientFunds_returnsOk() {
        when(validationService.validateFunds(any(ValidateFundsRequest.class)))
                .thenReturn(buildFundsValidResponse());

        given()
                .contentType(ContentType.JSON)
                .body(buildValidateFundsRequest())
                .when()
                .post("/v1/accounts/validate/funds")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(true))
                .body("accountNumber", equalTo(ACCOUNT_NUMBER))
                .body("requestedAmount", equalTo(1500.0f))
                .body("availableBalance", equalTo(5000.0f))
                .body("message", equalTo("Sufficient funds available"));

        verify(validationService).validateFunds(any(ValidateFundsRequest.class));
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void validateFunds_withRoleAdmin_returnsOk() {
        when(validationService.validateFunds(any(ValidateFundsRequest.class)))
                .thenReturn(buildFundsValidResponse());

        given()
                .contentType(ContentType.JSON)
                .body(buildValidateFundsRequest())
                .when()
                .post("/v1/accounts/validate/funds")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(true));

        verify(validationService).validateFunds(any(ValidateFundsRequest.class));
    }

    @Test
    void validateFunds_unauthorized_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(buildValidateFundsRequest())
                .when()
                .post("/v1/accounts/validate/funds")
                .then()
                .statusCode(401);

        verifyNoInteractions(validationService);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateFunds_insufficientFunds_returnsValidFalse() {
        when(validationService.validateFunds(any(ValidateFundsRequest.class)))
                .thenReturn(buildFundsInvalidResponse());

        given()
                .contentType(ContentType.JSON)
                .body(buildValidateFundsRequest())
                .when()
                .post("/v1/accounts/validate/funds")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(false))
                .body("message", equalTo("Insufficient funds"));

        verify(validationService).validateFunds(any(ValidateFundsRequest.class));
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateFunds_validationError_returnsBadRequest() {
        ValidateFundsRequest invalidRequest = new ValidateFundsRequest();

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post("/v1/accounts/validate/funds")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"));

        verifyNoInteractions(validationService);
    }

    // GET /v1/accounts/{accountNumber}/validate

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateAccount_activeAccount_returnsOk() {
        when(validationService.validateAccount(ACCOUNT_NUMBER))
                .thenReturn(buildAccountValidResponse());

        given()
                .when()
                .get("/v1/accounts/{accountNumber}/validate", ACCOUNT_NUMBER)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(true))
                .body("accountNumber", equalTo(ACCOUNT_NUMBER))
                .body("exists", equalTo(true))
                .body("status", equalTo("ACTIVE"))
                .body("message", equalTo("Account is active and valid"));

        verify(validationService).validateAccount(ACCOUNT_NUMBER);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void validateAccount_withRoleAdmin_returnsOk() {
        when(validationService.validateAccount(ACCOUNT_NUMBER))
                .thenReturn(buildAccountValidResponse());

        given()
                .when()
                .get("/v1/accounts/{accountNumber}/validate", ACCOUNT_NUMBER)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(true));

        verify(validationService).validateAccount(ACCOUNT_NUMBER);
    }

    @Test
    void validateAccount_unauthorized_returns401() {
        given()
                .when()
                .get("/v1/accounts/{accountNumber}/validate", ACCOUNT_NUMBER)
                .then()
                .statusCode(401);

        verifyNoInteractions(validationService);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateAccount_blockedAccount_returnsValidFalse() {
        when(validationService.validateAccount(ACCOUNT_NUMBER))
                .thenReturn(buildAccountInvalidResponse());

        given()
                .when()
                .get("/v1/accounts/{accountNumber}/validate", ACCOUNT_NUMBER)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(false))
                .body("exists", equalTo(true))
                .body("status", equalTo("BLOCKED"))
                .body("message", containsString("not active"));

        verify(validationService).validateAccount(ACCOUNT_NUMBER);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void validateAccount_notFound_returnsValidFalse() {
        String unknownNumber = "9999999999999999";
        when(validationService.validateAccount(unknownNumber))
                .thenReturn(buildAccountNotFoundResponse());

        given()
                .when()
                .get("/v1/accounts/{accountNumber}/validate", unknownNumber)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("valid", equalTo(false))
                .body("exists", equalTo(false))
                .body("message", equalTo("Account not found"));

        verify(validationService).validateAccount(unknownNumber);
    }
}
