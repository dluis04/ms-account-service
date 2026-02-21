package com.challengebank.account.model.dto.request;

import com.challengebank.account.model.enums.BalanceOperation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateBalanceRequest {

    @NotNull
    public Double amount;

    @NotNull
    public BalanceOperation operation;

    @Size(max = 500)
    public String description;

    public String referenceId;
}
