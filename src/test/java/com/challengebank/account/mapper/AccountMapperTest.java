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
import com.challengebank.account.model.enums.AccountType;
import com.challengebank.account.model.enums.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountMapperTest {

    private final AccountMapper mapper = new AccountMapper();

    @Test
    void testToEntity() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.customerId = UUID.randomUUID();
        request.accountType = AccountType.CHECKING;
        request.initialDeposit = 1000.0;
        request.currency = "EUR";
        request.overdraftLimit = 500.0;

        Account account = mapper.toEntity(request);

        assertEquals(request.customerId, account.customerId);
        assertEquals(AccountType.CHECKING, account.accountType);
        assertEquals(1000.0, account.balance);
        assertEquals(1000.0, account.availableBalance);
        assertEquals("EUR", account.currency);
        assertEquals(AccountStatus.ACTIVE, account.status);
        assertEquals(500.0, account.overdraftLimit);
        assertEquals(0.0, account.interestRate);
        assertNotNull(account.accountNumber);
    }

    @Test
    void testToEntity_defaultCurrencyAndOverdraft() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.customerId = UUID.randomUUID();
        request.accountType = AccountType.SAVINGS;
        request.initialDeposit = 500.0;
        // currency and overdraftLimit are null

        Account account = mapper.toEntity(request);

        assertEquals("USD", account.currency);
        assertEquals(0.0, account.overdraftLimit);
    }

    @Test
    void testToResponse() {
        Account account = new Account();
        account.accountId = UUID.randomUUID();
        account.accountNumber = "1234567890123456";
        account.customerId = UUID.randomUUID();
        account.accountType = AccountType.BUSINESS;
        account.balance = 25000.0;
        account.availableBalance = 24500.0;
        account.currency = "USD";
        account.status = AccountStatus.ACTIVE;
        account.overdraftLimit = 1000.0;
        account.interestRate = 1.5;
        account.createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        account.updatedAt = LocalDateTime.of(2024, 6, 1, 12, 0, 0);
        account.lastTransactionDate = LocalDateTime.of(2024, 6, 1, 11, 0, 0);

        AccountResponse response = mapper.toResponse(account);

        assertEquals(account.accountId, response.accountId);
        assertEquals("1234567890123456", response.accountNumber);
        assertEquals(account.customerId, response.customerId);
        assertEquals(AccountType.BUSINESS, response.accountType);
        assertEquals(25000.0, response.balance);
        assertEquals(24500.0, response.availableBalance);
        assertEquals("USD", response.currency);
        assertEquals(AccountStatus.ACTIVE, response.status);
        assertEquals(1000.0, response.overdraftLimit);
        assertEquals(1.5, response.interestRate);
        assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0, 0), response.createdAt);
        assertEquals(LocalDateTime.of(2024, 6, 1, 12, 0, 0), response.updatedAt);
        assertEquals(LocalDateTime.of(2024, 6, 1, 11, 0, 0), response.lastTransactionDate);
    }

    @Test
    void testUpdateEntity_allFields() {
        Account account = new Account();
        account.overdraftLimit = 100.0;
        account.interestRate = 1.0;

        UpdateAccountRequest request = new UpdateAccountRequest();
        request.overdraftLimit = 500.0;
        request.interestRate = 2.5;

        mapper.updateEntity(account, request);

        assertEquals(500.0, account.overdraftLimit);
        assertEquals(2.5, account.interestRate);
    }

    @Test
    void testUpdateEntity_partialFields() {
        Account account = new Account();
        account.overdraftLimit = 100.0;
        account.interestRate = 1.0;

        UpdateAccountRequest request = new UpdateAccountRequest();
        request.overdraftLimit = 300.0;
        // interestRate is null

        mapper.updateEntity(account, request);

        assertEquals(300.0, account.overdraftLimit);
        assertEquals(1.0, account.interestRate);
    }

    @Test
    void testUpdateEntity_noFields() {
        Account account = new Account();
        account.overdraftLimit = 100.0;
        account.interestRate = 1.0;

        UpdateAccountRequest request = new UpdateAccountRequest();

        mapper.updateEntity(account, request);

        assertEquals(100.0, account.overdraftLimit);
        assertEquals(1.0, account.interestRate);
    }

    @Test
    void testToPageResponse() {
        Account a1 = new Account();
        a1.accountId = UUID.randomUUID();
        a1.accountNumber = "1111111111111111";
        a1.customerId = UUID.randomUUID();
        a1.accountType = AccountType.CHECKING;
        a1.balance = 1000.0;
        a1.availableBalance = 1000.0;
        a1.currency = "USD";
        a1.status = AccountStatus.ACTIVE;

        Account a2 = new Account();
        a2.accountId = UUID.randomUUID();
        a2.accountNumber = "2222222222222222";
        a2.customerId = UUID.randomUUID();
        a2.accountType = AccountType.SAVINGS;
        a2.balance = 5000.0;
        a2.availableBalance = 5000.0;
        a2.currency = "USD";
        a2.status = AccountStatus.ACTIVE;

        List<Account> accounts = List.of(a1, a2);

        AccountPageResponse response = mapper.toPageResponse(accounts, 0, 10, 25);

        assertEquals(2, response.content.size());
        assertEquals(0, response.page);
        assertEquals(10, response.size);
        assertEquals(25, response.totalElements);
        assertEquals(3, response.totalPages);

        assertEquals("1111111111111111", response.content.get(0).accountNumber);
        assertEquals("2222222222222222", response.content.get(1).accountNumber);
    }

    @Test
    void testToPageResponse_emptyList() {
        List<Account> accounts = Collections.emptyList();

        AccountPageResponse response = mapper.toPageResponse(accounts, 0, 10, 0);

        assertTrue(response.content.isEmpty());
        assertEquals(0, response.page);
        assertEquals(10, response.size);
        assertEquals(0, response.totalElements);
        assertEquals(0, response.totalPages);
    }

    @Test
    void testToPageResponse_sizeZero() {
        List<Account> accounts = Collections.emptyList();

        AccountPageResponse response = mapper.toPageResponse(accounts, 0, 0, 0);

        assertTrue(response.content.isEmpty());
        assertEquals(0, response.totalPages);
    }

    @Test
    void testToBalanceResponse_withUpdatedAt() {
        Account account = new Account();
        account.accountNumber = "1234567890123456";
        account.balance = 10000.0;
        account.availableBalance = 9500.0;
        account.currency = "USD";
        account.createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        account.updatedAt = LocalDateTime.of(2024, 6, 1, 12, 0, 0);

        BalanceResponse response = mapper.toBalanceResponse(account, 500.0);

        assertEquals("1234567890123456", response.accountNumber);
        assertEquals(10000.0, response.balance);
        assertEquals(9500.0, response.availableBalance);
        assertEquals(500.0, response.reservedAmount);
        assertEquals("USD", response.currency);
        assertEquals(LocalDateTime.of(2024, 6, 1, 12, 0, 0), response.lastUpdated);
    }

    @Test
    void testToBalanceResponse_withoutUpdatedAt() {
        Account account = new Account();
        account.accountNumber = "1234567890123456";
        account.balance = 10000.0;
        account.availableBalance = 10000.0;
        account.currency = "USD";
        account.createdAt = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        account.updatedAt = null;

        BalanceResponse response = mapper.toBalanceResponse(account, 0.0);

        assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0, 0), response.lastUpdated);
    }

    @Test
    void testToReservationResponse() {
        FundReservation reservation = new FundReservation();
        reservation.reservationId = UUID.randomUUID();
        reservation.accountNumber = "1234567890123456";
        reservation.amount = 500.0;
        reservation.expiresAt = LocalDateTime.of(2024, 6, 1, 15, 0, 0);
        reservation.status = ReservationStatus.ACTIVE;

        ReservationResponse response = mapper.toReservationResponse(reservation);

        assertEquals(reservation.reservationId, response.reservationId);
        assertEquals("1234567890123456", response.accountNumber);
        assertEquals(500.0, response.amount);
        assertEquals(LocalDateTime.of(2024, 6, 1, 15, 0, 0), response.expiresAt);
        assertEquals(ReservationStatus.ACTIVE, response.status);
    }

    @Test
    void testGenerateAccountNumber() {
        String number = mapper.generateAccountNumber();

        assertNotNull(number);
        assertEquals(16, number.length());
        assertTrue(number.matches("^[0-9]+$"));
    }
}
