package com.challengebank.account.model.dto.request;

import com.challengebank.account.model.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateStatusRequest {

    @NotNull
    public AccountStatus status;

    @Size(max = 500)
    public String reason;
}
