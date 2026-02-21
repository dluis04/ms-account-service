package com.challengebank.account.model.dto.response;

public class FundsValidationResponse {

    public boolean valid;
    public String accountNumber;
    public double requestedAmount;
    public double availableBalance;
    public double overdraftAvailable;
    public double totalAvailable;
    public String message;
}
