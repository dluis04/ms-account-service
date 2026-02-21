package com.challengebank.account.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class ValidateFundsRequest {

    @NotBlank
    @Pattern(regexp = "^[0-9]{10,20}$")
    public String accountNumber;

    @NotNull
    @DecimalMin("0.01")
    public Double amount;

    public Boolean includeOverdraft;
}
