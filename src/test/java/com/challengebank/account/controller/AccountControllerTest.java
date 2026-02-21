package com.challengebank.account.controller;

import com.challengebank.account.exception.AccountNotFoundException;
import com.challengebank.account.exception.InvalidAccountOperationException;
import com.challengebank.account.model.dto.request.CreateAccountRequest;
import com.challengebank.account.model.dto.request.UpdateAccountRequest;
import com.challengebank.account.model.dto.request.UpdateStatusRequest;
import com.challengebank.account.model.dto.response.AccountPageResponse;
import com.challengebank.account.model.dto.response.AccountResponse;
import com.challengebank.account.model.enums.AccountStatus;
import com.challengebank.account.model.enums.AccountType;
import com.challengebank.account.service.AccountService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@QuarkusTest
class AccountControllerTest {

    @InjectMock
    AccountService accountService;

    private static final String ACCOUNT_NUMBER = "1234567890123456";
    private static final UUID ACCOUNT_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final UUID CUSTOMER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private CreateAccountRequest buildCreateRequest() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.customerId = CUSTOMER_ID;
        request.accountType = AccountType.CHECKING;
        request.initialDeposit = 1000.0;
        request.currency = "USD";
        request.overdraftLimit = 500.0;
        return request;
    }

    private UpdateAccountRequest buildUpdateRequest() {
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.overdraftLimit = 1000.0;
        request.interestRate = 2.5;
        return request;
    }

    private UpdateStatusRequest buildUpdateStatusRequest() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.status = AccountStatus.BLOCKED;
        request.reason = "Suspicious activity";
        return request;
    }

    private AccountResponse buildAccountResponse() {
        AccountResponse response = new AccountResponse();
        response.accountId = ACCOUNT_ID;
        response.accountNumber = ACCOUNT_NUMBER;
        response.customerId = CUSTOMER_ID;
        response.accountType = AccountType.CHECKING;
        response.balance = 1000.0;
        response.availableBalance = 1000.0;
        response.currency = "USD";
        response.status = AccountStatus.ACTIVE;
        response.overdraftLimit = 500.0;
        response.interestRate = 0.0;
        response.createdAt = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        response.updatedAt = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        return response;
    }

    private AccountPageResponse buildPageResponse() {
        AccountPageResponse page = new AccountPageResponse();
        page.content = List.of(buildAccountResponse());
        page.page = 0;
        page.size = 20;
        page.totalElements = 1;
        page.totalPages = 1;
        return page;
    }

    private AccountPageResponse buildEmptyPageResponse() {
        AccountPageResponse page = new AccountPageResponse();
        page.content = Collections.emptyList();
        page.page = 0;
        page.size = 20;
        page.totalElements = 0;
        page.totalPages = 0;
        return page;
    }

    // GET /v1/accounts

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void getAllAccounts_withRoleUser_returnsOk() {
        when(accountService.getAllAccounts(anyInt(), anyInt(), any(), any()))
                .thenReturn(buildPageResponse());

        given()
                .when()
                .get("/v1/accounts")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", hasSize(1))
                .body("content[0].accountNumber", equalTo(ACCOUNT_NUMBER))
                .body("page", equalTo(0))
                .body("totalElements", equalTo(1));

        verify(accountService).getAllAccounts(0, 20, null, null);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void getAllAccounts_withRoleAdmin_returnsOk() {
        when(accountService.getAllAccounts(anyInt(), anyInt(), any(), any()))
                .thenReturn(buildPageResponse());

        given()
                .when()
                .get("/v1/accounts")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", hasSize(1));

        verify(accountService).getAllAccounts(0, 20, null, null);
    }

    @Test
    void getAllAccounts_unauthorized_returns401() {
        given()
                .when()
                .get("/v1/accounts")
                .then()
                .statusCode(401);

        verifyNoInteractions(accountService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void getAllAccounts_withStatusFilter_returnsFiltered() {
        when(accountService.getAllAccounts(eq(0), eq(20), eq(AccountStatus.ACTIVE), any()))
                .thenReturn(buildPageResponse());

        given()
                .queryParam("status", "ACTIVE")
                .when()
                .get("/v1/accounts")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", hasSize(1));

        verify(accountService).getAllAccounts(0, 20, AccountStatus.ACTIVE, null);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void getAllAccounts_withPagination_returnsPaged() {
        when(accountService.getAllAccounts(eq(2), eq(10), any(), any()))
                .thenReturn(buildEmptyPageResponse());

        given()
                .queryParam("page", 2)
                .queryParam("size", 10)
                .when()
                .get("/v1/accounts")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", hasSize(0));

        verify(accountService).getAllAccounts(2, 10, null, null);
    }

    // POST /v1/accounts

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void createAccount_withRoleAdmin_returnsCreated() {
        when(accountService.createAccount(any(CreateAccountRequest.class)))
                .thenReturn(buildAccountResponse());

        given()
                .contentType(ContentType.JSON)
                .body(buildCreateRequest())
                .when()
                .post("/v1/accounts")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("accountId", equalTo(ACCOUNT_ID.toString()))
                .body("accountNumber", equalTo(ACCOUNT_NUMBER))
                .body("accountType", equalTo("CHECKING"))
                .body("status", equalTo("ACTIVE"));

        verify(accountService).createAccount(any(CreateAccountRequest.class));
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void createAccount_withRoleUser_returnsForbidden() {
        given()
                .contentType(ContentType.JSON)
                .body(buildCreateRequest())
                .when()
                .post("/v1/accounts")
                .then()
                .statusCode(403);

        verifyNoInteractions(accountService);
    }

    @Test
    void createAccount_unauthorized_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(buildCreateRequest())
                .when()
                .post("/v1/accounts")
                .then()
                .statusCode(401);

        verifyNoInteractions(accountService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void createAccount_validationError_returnsBadRequest() {
        CreateAccountRequest invalidRequest = new CreateAccountRequest();

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .post("/v1/accounts")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("errors", is(notNullValue()))
                .body("errors.size()", greaterThan(0));

        verifyNoInteractions(accountService);
    }

    // GET /v1/accounts/{accountNumber}

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void getAccountByNumber_success_returnsOk() {
        when(accountService.getAccountByNumber(ACCOUNT_NUMBER))
                .thenReturn(buildAccountResponse());

        given()
                .when()
                .get("/v1/accounts/{accountNumber}", ACCOUNT_NUMBER)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("accountNumber", equalTo(ACCOUNT_NUMBER))
                .body("status", equalTo("ACTIVE"));

        verify(accountService).getAccountByNumber(ACCOUNT_NUMBER);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void getAccountByNumber_notFound_returns404() {
        String unknownNumber = "9999999999999999";
        when(accountService.getAccountByNumber(unknownNumber))
                .thenThrow(new AccountNotFoundException("Account not found: " + unknownNumber));

        given()
                .when()
                .get("/v1/accounts/{accountNumber}", unknownNumber)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", containsString("Account not found"));

        verify(accountService).getAccountByNumber(unknownNumber);
    }

    @Test
    void getAccountByNumber_unauthorized_returns401() {
        given()
                .when()
                .get("/v1/accounts/{accountNumber}", ACCOUNT_NUMBER)
                .then()
                .statusCode(401);

        verifyNoInteractions(accountService);
    }

    // PUT /v1/accounts/{accountNumber}

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateAccount_withRoleAdmin_returnsOk() {
        AccountResponse updatedResponse = buildAccountResponse();
        updatedResponse.overdraftLimit = 1000.0;
        updatedResponse.interestRate = 2.5;

        when(accountService.updateAccount(eq(ACCOUNT_NUMBER), any(UpdateAccountRequest.class)))
                .thenReturn(updatedResponse);

        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateRequest())
                .when()
                .put("/v1/accounts/{accountNumber}", ACCOUNT_NUMBER)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("accountNumber", equalTo(ACCOUNT_NUMBER))
                .body("overdraftLimit", equalTo(1000.0f));

        verify(accountService).updateAccount(eq(ACCOUNT_NUMBER), any(UpdateAccountRequest.class));
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void updateAccount_withRoleUser_returnsForbidden() {
        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateRequest())
                .when()
                .put("/v1/accounts/{accountNumber}", ACCOUNT_NUMBER)
                .then()
                .statusCode(403);

        verifyNoInteractions(accountService);
    }

    @Test
    void updateAccount_unauthorized_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateRequest())
                .when()
                .put("/v1/accounts/{accountNumber}", ACCOUNT_NUMBER)
                .then()
                .statusCode(401);

        verifyNoInteractions(accountService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateAccount_notFound_returns404() {
        String unknownNumber = "9999999999999999";
        when(accountService.updateAccount(eq(unknownNumber), any(UpdateAccountRequest.class)))
                .thenThrow(new AccountNotFoundException("Account not found: " + unknownNumber));

        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateRequest())
                .when()
                .put("/v1/accounts/{accountNumber}", unknownNumber)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("message", containsString("Account not found"));

        verify(accountService).updateAccount(eq(unknownNumber), any(UpdateAccountRequest.class));
    }

    // DELETE /v1/accounts/{accountNumber}

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void closeAccount_withRoleAdmin_returnsNoContent() {
        doNothing().when(accountService).closeAccount(ACCOUNT_NUMBER);

        given()
                .when()
                .delete("/v1/accounts/{accountNumber}", ACCOUNT_NUMBER)
                .then()
                .statusCode(204);

        verify(accountService).closeAccount(ACCOUNT_NUMBER);
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void closeAccount_withRoleUser_returnsForbidden() {
        given()
                .when()
                .delete("/v1/accounts/{accountNumber}", ACCOUNT_NUMBER)
                .then()
                .statusCode(403);

        verifyNoInteractions(accountService);
    }

    @Test
    void closeAccount_unauthorized_returns401() {
        given()
                .when()
                .delete("/v1/accounts/{accountNumber}", ACCOUNT_NUMBER)
                .then()
                .statusCode(401);

        verifyNoInteractions(accountService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void closeAccount_notFound_returns404() {
        String unknownNumber = "9999999999999999";
        doThrow(new AccountNotFoundException("Account not found: " + unknownNumber))
                .when(accountService).closeAccount(unknownNumber);

        given()
                .when()
                .delete("/v1/accounts/{accountNumber}", unknownNumber)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("message", containsString("Account not found"));

        verify(accountService).closeAccount(unknownNumber);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void closeAccount_nonZeroBalance_returnsBadRequest() {
        doThrow(new InvalidAccountOperationException("Cannot close account with non-zero balance: 500.0"))
                .when(accountService).closeAccount(ACCOUNT_NUMBER);

        given()
                .when()
                .delete("/v1/accounts/{accountNumber}", ACCOUNT_NUMBER)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("message", containsString("non-zero balance"));

        verify(accountService).closeAccount(ACCOUNT_NUMBER);
    }

    // GET /v1/accounts/customer/{customerId}

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void getAccountsByCustomerId_success_returnsOk() {
        when(accountService.getAccountsByCustomerId(CUSTOMER_ID))
                .thenReturn(List.of(buildAccountResponse()));

        given()
                .when()
                .get("/v1/accounts/customer/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("[0].accountNumber", equalTo(ACCOUNT_NUMBER));

        verify(accountService).getAccountsByCustomerId(CUSTOMER_ID);
    }

    @Test
    void getAccountsByCustomerId_unauthorized_returns401() {
        given()
                .when()
                .get("/v1/accounts/customer/{customerId}", CUSTOMER_ID)
                .then()
                .statusCode(401);

        verifyNoInteractions(accountService);
    }

    // PATCH /v1/accounts/{accountNumber}/status

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateAccountStatus_withRoleAdmin_returnsOk() {
        AccountResponse blockedResponse = buildAccountResponse();
        blockedResponse.status = AccountStatus.BLOCKED;

        when(accountService.updateAccountStatus(eq(ACCOUNT_NUMBER), any(UpdateStatusRequest.class)))
                .thenReturn(blockedResponse);

        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateStatusRequest())
                .when()
                .patch("/v1/accounts/{accountNumber}/status", ACCOUNT_NUMBER)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("accountNumber", equalTo(ACCOUNT_NUMBER))
                .body("status", equalTo("BLOCKED"));

        verify(accountService).updateAccountStatus(eq(ACCOUNT_NUMBER), any(UpdateStatusRequest.class));
    }

    @Test
    @TestSecurity(user = "user1", roles = "ROLE_USER")
    void updateAccountStatus_withRoleUser_returnsForbidden() {
        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateStatusRequest())
                .when()
                .patch("/v1/accounts/{accountNumber}/status", ACCOUNT_NUMBER)
                .then()
                .statusCode(403);

        verifyNoInteractions(accountService);
    }

    @Test
    void updateAccountStatus_unauthorized_returns401() {
        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateStatusRequest())
                .when()
                .patch("/v1/accounts/{accountNumber}/status", ACCOUNT_NUMBER)
                .then()
                .statusCode(401);

        verifyNoInteractions(accountService);
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateAccountStatus_notFound_returns404() {
        String unknownNumber = "9999999999999999";
        when(accountService.updateAccountStatus(eq(unknownNumber), any(UpdateStatusRequest.class)))
                .thenThrow(new AccountNotFoundException("Account not found: " + unknownNumber));

        given()
                .contentType(ContentType.JSON)
                .body(buildUpdateStatusRequest())
                .when()
                .patch("/v1/accounts/{accountNumber}/status", unknownNumber)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("status", equalTo(404))
                .body("message", containsString("Account not found"));

        verify(accountService).updateAccountStatus(eq(unknownNumber), any(UpdateStatusRequest.class));
    }

    @Test
    @TestSecurity(user = "admin1", roles = "ROLE_ADMIN")
    void updateAccountStatus_validationError_returnsBadRequest() {
        UpdateStatusRequest invalidRequest = new UpdateStatusRequest();
        invalidRequest.status = null;

        given()
                .contentType(ContentType.JSON)
                .body(invalidRequest)
                .when()
                .patch("/v1/accounts/{accountNumber}/status", ACCOUNT_NUMBER)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"));

        verifyNoInteractions(accountService);
    }
}
