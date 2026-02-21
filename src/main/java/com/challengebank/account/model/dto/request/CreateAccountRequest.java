package com.challengebank.account.model.dto.request;

import com.challengebank.account.model.enums.AccountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateAccountRequest {

    @NotNull
    public UUID customerId;

    @NotNull
    public AccountType accountType;

    @NotNull
    @Min(0)
    public Double initialDeposit;

    public String currency;

    @Min(0)
    public Double overdraftLimit;
}
