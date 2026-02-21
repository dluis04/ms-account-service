package com.challengebank.account.model.dto.response;

import java.time.LocalDateTime;

public class BalanceResponse {

    public String accountNumber;
    public double balance;
    public double availableBalance;
    public double reservedAmount;
    public String currency;
    public LocalDateTime lastUpdated;
}
