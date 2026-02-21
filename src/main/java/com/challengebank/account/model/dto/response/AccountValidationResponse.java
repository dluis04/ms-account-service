package com.challengebank.account.model.dto.response;

import com.challengebank.account.model.enums.AccountStatus;

public class AccountValidationResponse {

    public boolean valid;
    public String accountNumber;
    public boolean exists;
    public AccountStatus status;
    public String message;
}
