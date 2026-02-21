package com.challengebank.account.service;

import com.challengebank.account.model.dto.request.ValidateFundsRequest;
import com.challengebank.account.model.dto.response.AccountValidationResponse;
import com.challengebank.account.model.dto.response.FundsValidationResponse;
import com.challengebank.account.model.entity.Account;
import com.challengebank.account.model.enums.AccountStatus;
import com.challengebank.account.repository.AccountRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    MeterRegistry meterRegistry;

    @Mock
    Counter validationSuccessCounter;

    @Mock
    Counter validationFailureCounter;

    @InjectMocks
    ValidationService validationService;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter("account.validation.success")).thenReturn(validationSuccessCounter);
        when(meterRegistry.counter("account.validation.failure")).thenReturn(validationFailureCounter);
        validationService.initMetrics();
    }

    @Test
    void testValidateFunds_sufficientFunds() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.availableBalance = 5000.0;
        account.overdraftLimit = 500.0;

        ValidateFundsRequest request = new ValidateFundsRequest();
        request.accountNumber = accountNumber;
        request.amount = 3000.0;
        request.includeOverdraft = false;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        FundsValidationResponse response = validationService.validateFunds(request);

        assertTrue(response.valid);
        assertEquals(accountNumber, response.accountNumber);
        assertEquals(3000.0, response.requestedAmount);
        assertEquals(5000.0, response.availableBalance);
        assertEquals(500.0, response.overdraftAvailable);
        assertEquals(5000.0, response.totalAvailable);
        assertEquals("Sufficient funds available", response.message);
        verify(validationSuccessCounter).increment();
    }

    @Test
    void testValidateFunds_sufficientWithOverdraft() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.availableBalance = 3000.0;
        account.overdraftLimit = 2000.0;

        ValidateFundsRequest request = new ValidateFundsRequest();
        request.accountNumber = accountNumber;
        request.amount = 4000.0;
        request.includeOverdraft = true;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        FundsValidationResponse response = validationService.validateFunds(request);

        assertTrue(response.valid);
        assertEquals(5000.0, response.totalAvailable);
        assertEquals("Sufficient funds available", response.message);
        verify(validationSuccessCounter).increment();
    }

    @Test
    void testValidateFunds_insufficientFunds() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.availableBalance = 1000.0;
        account.overdraftLimit = 0.0;

        ValidateFundsRequest request = new ValidateFundsRequest();
        request.accountNumber = accountNumber;
        request.amount = 5000.0;
        request.includeOverdraft = false;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        FundsValidationResponse response = validationService.validateFunds(request);

        assertFalse(response.valid);
        assertEquals("Insufficient funds", response.message);
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateFunds_insufficientWithOverdraft() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.availableBalance = 1000.0;
        account.overdraftLimit = 500.0;

        ValidateFundsRequest request = new ValidateFundsRequest();
        request.accountNumber = accountNumber;
        request.amount = 2000.0;
        request.includeOverdraft = true;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        FundsValidationResponse response = validationService.validateFunds(request);

        assertFalse(response.valid);
        assertEquals(1500.0, response.totalAvailable);
        assertEquals("Insufficient funds", response.message);
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateFunds_accountNotFound() {
        String accountNumber = "9999999999999999";

        ValidateFundsRequest request = new ValidateFundsRequest();
        request.accountNumber = accountNumber;
        request.amount = 1000.0;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        FundsValidationResponse response = validationService.validateFunds(request);

        assertFalse(response.valid);
        assertEquals("Account not found", response.message);
        assertEquals(accountNumber, response.accountNumber);
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateFunds_nullIncludeOverdraft() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.availableBalance = 5000.0;
        account.overdraftLimit = 500.0;

        ValidateFundsRequest request = new ValidateFundsRequest();
        request.accountNumber = accountNumber;
        request.amount = 3000.0;
        request.includeOverdraft = null;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        FundsValidationResponse response = validationService.validateFunds(request);

        assertTrue(response.valid);
        assertEquals(5000.0, response.totalAvailable);
        verify(validationSuccessCounter).increment();
    }

    @Test
    void testValidateAccount_activeAccount() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.status = AccountStatus.ACTIVE;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        AccountValidationResponse response = validationService.validateAccount(accountNumber);

        assertTrue(response.valid);
        assertTrue(response.exists);
        assertEquals(accountNumber, response.accountNumber);
        assertEquals(AccountStatus.ACTIVE, response.status);
        assertEquals("Account is active and valid", response.message);
        verify(validationSuccessCounter).increment();
    }

    @Test
    void testValidateAccount_blockedAccount() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.status = AccountStatus.BLOCKED;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        AccountValidationResponse response = validationService.validateAccount(accountNumber);

        assertFalse(response.valid);
        assertTrue(response.exists);
        assertEquals(AccountStatus.BLOCKED, response.status);
        assertTrue(response.message.contains("not active"));
        assertTrue(response.message.contains("BLOCKED"));
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateAccount_closedAccount() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.status = AccountStatus.CLOSED;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        AccountValidationResponse response = validationService.validateAccount(accountNumber);

        assertFalse(response.valid);
        assertTrue(response.exists);
        assertEquals(AccountStatus.CLOSED, response.status);
        assertTrue(response.message.contains("not active"));
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateAccount_inactiveAccount() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.status = AccountStatus.INACTIVE;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        AccountValidationResponse response = validationService.validateAccount(accountNumber);

        assertFalse(response.valid);
        assertTrue(response.exists);
        assertEquals(AccountStatus.INACTIVE, response.status);
        assertTrue(response.message.contains("INACTIVE"));
        verify(validationFailureCounter).increment();
    }

    @Test
    void testValidateAccount_accountNotFound() {
        String accountNumber = "9999999999999999";

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        AccountValidationResponse response = validationService.validateAccount(accountNumber);

        assertFalse(response.valid);
        assertFalse(response.exists);
        assertEquals("Account not found", response.message);
        verify(validationFailureCounter).increment();
    }
}
