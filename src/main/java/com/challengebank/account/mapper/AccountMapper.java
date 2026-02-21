package com.challengebank.account.mapper;

import com.challengebank.account.model.dto.request.CreateAccountRequest;
import com.challengebank.account.model.dto.request.UpdateAccountRequest;
import com.challengebank.account.model.dto.response.AccountPageResponse;
import com.challengebank.account.model.dto.response.AccountResponse;
import com.challengebank.account.model.dto.response.BalanceResponse;
import com.challengebank.account.model.dto.response.ReservationResponse;
import com.challengebank.account.model.entity.Account;
import com.challengebank.account.model.entity.FundReservation;
import com.challengebank.account.model.enums.AccountStatus;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class AccountMapper {

    public Account toEntity(CreateAccountRequest request) {
        Account account = new Account();
        account.customerId = request.customerId;
        account.accountType = request.accountType;
        account.balance = request.initialDeposit;
        account.availableBalance = request.initialDeposit;
        account.currency = request.currency != null ? request.currency : "USD";
        account.status = AccountStatus.ACTIVE;
        account.overdraftLimit = request.overdraftLimit != null ? request.overdraftLimit : 0.0;
        account.interestRate = 0.0;
        account.accountNumber = generateAccountNumber();
        return account;
    }

    public void updateEntity(Account account, UpdateAccountRequest request) {
        if (request.overdraftLimit != null) {
            account.overdraftLimit = request.overdraftLimit;
        }
        if (request.interestRate != null) {
            account.interestRate = request.interestRate;
        }
    }

    public AccountResponse toResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.accountId = account.accountId;
        response.accountNumber = account.accountNumber;
        response.customerId = account.customerId;
        response.accountType = account.accountType;
        response.balance = account.balance;
        response.availableBalance = account.availableBalance;
        response.currency = account.currency;
        response.status = account.status;
        response.overdraftLimit = account.overdraftLimit;
        response.interestRate = account.interestRate;
        response.createdAt = account.createdAt;
        response.updatedAt = account.updatedAt;
        response.lastTransactionDate = account.lastTransactionDate;
        return response;
    }

    public AccountPageResponse toPageResponse(List<Account> accounts, int page, int size, long totalElements) {
        AccountPageResponse response = new AccountPageResponse();
        response.content = accounts.stream().map(this::toResponse).toList();
        response.page = page;
        response.size = size;
        response.totalElements = totalElements;
        response.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return response;
    }

    public BalanceResponse toBalanceResponse(Account account, double reservedAmount) {
        BalanceResponse response = new BalanceResponse();
        response.accountNumber = account.accountNumber;
        response.balance = account.balance;
        response.availableBalance = account.availableBalance;
        response.reservedAmount = reservedAmount;
        response.currency = account.currency;
        response.lastUpdated = account.updatedAt != null ? account.updatedAt : account.createdAt;
        return response;
    }

    public ReservationResponse toReservationResponse(FundReservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.reservationId = reservation.reservationId;
        response.accountNumber = reservation.accountNumber;
        response.amount = reservation.amount;
        response.expiresAt = reservation.expiresAt;
        response.status = reservation.status;
        return response;
    }

    public String generateAccountNumber() {
        long number = ThreadLocalRandom.current().nextLong(1000000000000000L, 9999999999999999L);
        return String.valueOf(number);
    }
}
