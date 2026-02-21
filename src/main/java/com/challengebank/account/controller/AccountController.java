package com.challengebank.account.controller;

import com.challengebank.account.model.dto.request.CreateAccountRequest;
import com.challengebank.account.model.dto.request.UpdateAccountRequest;
import com.challengebank.account.model.dto.request.UpdateStatusRequest;
import com.challengebank.account.model.dto.response.AccountPageResponse;
import com.challengebank.account.model.dto.response.AccountResponse;
import com.challengebank.account.model.enums.AccountStatus;
import com.challengebank.account.model.enums.AccountType;
import com.challengebank.account.service.AccountService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/v1/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountController {

    @Inject
    AccountService accountService;

    @GET
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response getAllAccounts(
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size,
            @QueryParam("status") AccountStatus status,
            @QueryParam("accountType") AccountType accountType) {
        AccountPageResponse response = accountService.getAllAccounts(page, size, status, accountType);
        return Response.ok(response).build();
    }

    @POST
    @RolesAllowed("ROLE_ADMIN")
    public Response createAccount(@Valid CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("/{accountNumber}")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response getAccountByNumber(@PathParam("accountNumber") String accountNumber) {
        AccountResponse response = accountService.getAccountByNumber(accountNumber);
        return Response.ok(response).build();
    }

    @PUT
    @Path("/{accountNumber}")
    @RolesAllowed("ROLE_ADMIN")
    public Response updateAccount(@PathParam("accountNumber") String accountNumber,
                                  @Valid UpdateAccountRequest request) {
        AccountResponse response = accountService.updateAccount(accountNumber, request);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{accountNumber}")
    @RolesAllowed("ROLE_ADMIN")
    public Response closeAccount(@PathParam("accountNumber") String accountNumber) {
        accountService.closeAccount(accountNumber);
        return Response.noContent().build();
    }

    @GET
    @Path("/customer/{customerId}")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response getAccountsByCustomerId(@PathParam("customerId") UUID customerId) {
        List<AccountResponse> response = accountService.getAccountsByCustomerId(customerId);
        return Response.ok(response).build();
    }

    @PATCH
    @Path("/{accountNumber}/status")
    @RolesAllowed("ROLE_ADMIN")
    public Response updateAccountStatus(@PathParam("accountNumber") String accountNumber,
                                        @Valid UpdateStatusRequest request) {
        AccountResponse response = accountService.updateAccountStatus(accountNumber, request);
        return Response.ok(response).build();
    }
}
