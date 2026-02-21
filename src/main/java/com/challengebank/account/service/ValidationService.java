package com.challengebank.account.service;

import com.challengebank.account.model.dto.request.ValidateFundsRequest;
import com.challengebank.account.model.dto.response.AccountValidationResponse;
import com.challengebank.account.model.dto.response.FundsValidationResponse;
import com.challengebank.account.model.entity.Account;
import com.challengebank.account.model.enums.AccountStatus;
import com.challengebank.account.repository.AccountRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class ValidationService {

    @Inject
    AccountRepository accountRepository;

    @Inject
    MeterRegistry meterRegistry;

    Counter validationSuccessCounter;
    Counter validationFailureCounter;

    @PostConstruct
    void initMetrics() {
        validationSuccessCounter = meterRegistry.counter("account.validation.success");
        validationFailureCounter = meterRegistry.counter("account.validation.failure");
    }

    public FundsValidationResponse validateFunds(ValidateFundsRequest request) {
        Optional<Account> accountOpt = accountRepository.findByAccountNumber(request.accountNumber);

        FundsValidationResponse response = new FundsValidationResponse();
        response.accountNumber = request.accountNumber;
        response.requestedAmount = request.amount;

        if (accountOpt.isEmpty()) {
            response.valid = false;
            response.message = "Account not found";
            validationFailureCounter.increment();
            return response;
        }

        Account account = accountOpt.get();
        response.availableBalance = account.availableBalance;
        response.overdraftAvailable = account.overdraftLimit;

        boolean includeOverdraft = request.includeOverdraft != null && request.includeOverdraft;
        double totalAvailable = includeOverdraft
                ? account.availableBalance + account.overdraftLimit
                : account.availableBalance;
        response.totalAvailable = totalAvailable;

        if (request.amount <= totalAvailable) {
            response.valid = true;
            response.message = "Sufficient funds available";
            validationSuccessCounter.increment();
        } else {
            response.valid = false;
            response.message = "Insufficient funds";
            validationFailureCounter.increment();
        }

        return response;
    }

    public AccountValidationResponse validateAccount(String accountNumber) {
        Optional<Account> accountOpt = accountRepository.findByAccountNumber(accountNumber);

        AccountValidationResponse response = new AccountValidationResponse();
        response.accountNumber = accountNumber;

        if (accountOpt.isEmpty()) {
            response.valid = false;
            response.exists = false;
            response.message = "Account not found";
            validationFailureCounter.increment();
        } else {
            Account account = accountOpt.get();
            response.exists = true;
            response.status = account.status;
            response.valid = account.status == AccountStatus.ACTIVE;
            response.message = response.valid
                    ? "Account is active and valid"
                    : "Account exists but is not active (status: " + account.status + ")";
            if (response.valid) {
                validationSuccessCounter.increment();
            } else {
                validationFailureCounter.increment();
            }
        }
        return response;
    }
}
