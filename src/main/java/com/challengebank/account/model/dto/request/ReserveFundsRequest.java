package com.challengebank.account.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReserveFundsRequest {

    @NotNull
    @DecimalMin("0.01")
    public Double amount;

    @Size(max = 500)
    public String description;

    @Min(1)
    @Max(1440)
    public Integer expirationMinutes;
}
