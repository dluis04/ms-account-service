package com.challengebank.account.controller;

import com.challengebank.account.model.dto.request.ValidateFundsRequest;
import com.challengebank.account.model.dto.response.AccountValidationResponse;
import com.challengebank.account.model.dto.response.FundsValidationResponse;
import com.challengebank.account.service.ValidationService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ValidationController {

    @Inject
    ValidationService validationService;

    @POST
    @Path("/validate/funds")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response validateFunds(@Valid ValidateFundsRequest request) {
        FundsValidationResponse response = validationService.validateFunds(request);
        return Response.ok(response).build();
    }

    @GET
    @Path("/{accountNumber}/validate")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response validateAccount(@PathParam("accountNumber") String accountNumber) {
        AccountValidationResponse response = validationService.validateAccount(accountNumber);
        return Response.ok(response).build();
    }
}
