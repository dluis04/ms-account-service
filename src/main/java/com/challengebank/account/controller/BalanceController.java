package com.challengebank.account.controller;

import com.challengebank.account.model.dto.request.ReserveFundsRequest;
import com.challengebank.account.model.dto.request.UpdateBalanceRequest;
import com.challengebank.account.model.dto.response.BalanceResponse;
import com.challengebank.account.model.dto.response.ReservationResponse;
import com.challengebank.account.service.BalanceService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/v1/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BalanceController {

    @Inject
    BalanceService balanceService;

    @GET
    @Path("/{accountNumber}/balance")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response getAccountBalance(@PathParam("accountNumber") String accountNumber) {
        BalanceResponse response = balanceService.getBalance(accountNumber);
        return Response.ok(response).build();
    }

    @POST
    @Path("/{accountNumber}/balance/update")
    @RolesAllowed("ROLE_ADMIN")
    public Response updateAccountBalance(@PathParam("accountNumber") String accountNumber,
                                         @Valid UpdateBalanceRequest request) {
        BalanceResponse response = balanceService.updateBalance(accountNumber, request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/{accountNumber}/reserve")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response reserveFunds(@PathParam("accountNumber") String accountNumber,
                                 @Valid ReserveFundsRequest request) {
        ReservationResponse response = balanceService.reserveFunds(accountNumber, request);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{accountNumber}/reservations/{reservationId}")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public Response releaseReservedFunds(@PathParam("accountNumber") String accountNumber,
                                         @PathParam("reservationId") UUID reservationId) {
        balanceService.releaseReservedFunds(accountNumber, reservationId);
        return Response.noContent().build();
    }
}
