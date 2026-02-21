package com.challengebank.account.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class UpdateAccountRequest {

    @Min(0)
    public Double overdraftLimit;

    @Min(0)
    @Max(100)
    public Double interestRate;
}
