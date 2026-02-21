package com.challengebank.account.model.dto.response;

import com.challengebank.account.model.enums.AccountStatus;
import com.challengebank.account.model.enums.AccountType;

import java.time.LocalDateTime;
import java.util.UUID;

public class AccountResponse {

    public UUID accountId;
    public String accountNumber;
    public UUID customerId;
    public AccountType accountType;
    public double balance;
    public double availableBalance;
    public String currency;
    public AccountStatus status;
    public double overdraftLimit;
    public double interestRate;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public LocalDateTime lastTransactionDate;
}
