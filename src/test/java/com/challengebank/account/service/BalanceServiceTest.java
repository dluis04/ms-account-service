package com.challengebank.account.service;

import com.challengebank.account.exception.AccountNotFoundException;
import com.challengebank.account.exception.InsufficientFundsException;
import com.challengebank.account.exception.InvalidAccountOperationException;
import com.challengebank.account.exception.ReservationNotFoundException;
import com.challengebank.account.mapper.AccountMapper;
import com.challengebank.account.model.dto.request.ReserveFundsRequest;
import com.challengebank.account.model.dto.request.UpdateBalanceRequest;
import com.challengebank.account.model.dto.response.BalanceResponse;
import com.challengebank.account.model.dto.response.ReservationResponse;
import com.challengebank.account.model.entity.Account;
import com.challengebank.account.model.entity.FundReservation;
import com.challengebank.account.model.enums.AccountStatus;
import com.challengebank.account.model.enums.BalanceOperation;
import com.challengebank.account.model.enums.ReservationStatus;
import com.challengebank.account.repository.AccountRepository;
import com.challengebank.account.repository.FundReservationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    FundReservationRepository fundReservationRepository;

    @Mock
    AccountMapper accountMapper;

    @Mock
    MeterRegistry meterRegistry;

    @Mock
    Counter transferSuccessCounter;

    @Mock
    Counter transferFailureCounter;

    @InjectMocks
    BalanceService balanceService;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter("account.transfer.success")).thenReturn(transferSuccessCounter);
        when(meterRegistry.counter("account.transfer.failure")).thenReturn(transferFailureCounter);
        balanceService.initMetrics();
    }

    @Test
    void testGetBalance_success() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = 10000.0;
        account.availableBalance = 9500.0;

        BalanceResponse expectedResponse = new BalanceResponse();
        expectedResponse.accountNumber = accountNumber;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(fundReservationRepository.sumActiveReservations(accountNumber)).thenReturn(500.0);
        when(accountMapper.toBalanceResponse(account, 500.0)).thenReturn(expectedResponse);

        BalanceResponse result = balanceService.getBalance(accountNumber);

        assertEquals(accountNumber, result.accountNumber);
    }

    @Test
    void testGetBalance_accountNotFound() {
        String accountNumber = "9999999999999999";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> balanceService.getBalance(accountNumber));
    }

    @Test
    void testUpdateBalance_deposit() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = 1000.0;
        account.availableBalance = 1000.0;
        account.status = AccountStatus.ACTIVE;

        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.amount = 500.0;
        request.operation = BalanceOperation.DEPOSIT;
        request.description = "ATM deposit";

        BalanceResponse expectedResponse = new BalanceResponse();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(fundReservationRepository.sumActiveReservations(accountNumber)).thenReturn(0.0);
        when(accountMapper.toBalanceResponse(account, 0.0)).thenReturn(expectedResponse);

        balanceService.updateBalance(accountNumber, request);

        assertEquals(1500.0, account.balance);
        assertEquals(1500.0, account.availableBalance);
        assertNotNull(account.lastTransactionDate);
        verify(accountRepository).persist(account);
        verify(transferSuccessCounter).increment();
    }

    @Test
    void testUpdateBalance_withdrawal() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = 1000.0;
        account.availableBalance = 1000.0;
        account.overdraftLimit = 0.0;
        account.status = AccountStatus.ACTIVE;

        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.amount = 500.0;
        request.operation = BalanceOperation.WITHDRAWAL;

        BalanceResponse expectedResponse = new BalanceResponse();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(fundReservationRepository.sumActiveReservations(accountNumber)).thenReturn(0.0);
        when(accountMapper.toBalanceResponse(account, 0.0)).thenReturn(expectedResponse);

        balanceService.updateBalance(accountNumber, request);

        assertEquals(500.0, account.balance);
        assertEquals(500.0, account.availableBalance);
        verify(transferSuccessCounter).increment();
    }

    @Test
    void testUpdateBalance_withdrawal_insufficientFunds() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = 100.0;
        account.availableBalance = 100.0;
        account.overdraftLimit = 0.0;
        account.status = AccountStatus.ACTIVE;

        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.amount = 500.0;
        request.operation = BalanceOperation.WITHDRAWAL;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class,
                () -> balanceService.updateBalance(accountNumber, request));
        verify(transferFailureCounter).increment();
    }

    @Test
    void testUpdateBalance_withdrawal_withOverdraft() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = 100.0;
        account.availableBalance = 100.0;
        account.overdraftLimit = 500.0;
        account.status = AccountStatus.ACTIVE;

        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.amount = 400.0;
        request.operation = BalanceOperation.WITHDRAWAL;

        BalanceResponse expectedResponse = new BalanceResponse();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(fundReservationRepository.sumActiveReservations(accountNumber)).thenReturn(0.0);
        when(accountMapper.toBalanceResponse(account, 0.0)).thenReturn(expectedResponse);

        balanceService.updateBalance(accountNumber, request);

        assertEquals(-300.0, account.balance);
        assertEquals(-300.0, account.availableBalance);
        verify(transferSuccessCounter).increment();
    }

    @Test
    void testUpdateBalance_adjustment() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = 1000.0;
        account.availableBalance = 1000.0;
        account.status = AccountStatus.ACTIVE;

        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.amount = -200.0;
        request.operation = BalanceOperation.ADJUSTMENT;

        BalanceResponse expectedResponse = new BalanceResponse();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(fundReservationRepository.sumActiveReservations(accountNumber)).thenReturn(0.0);
        when(accountMapper.toBalanceResponse(account, 0.0)).thenReturn(expectedResponse);

        balanceService.updateBalance(accountNumber, request);

        assertEquals(800.0, account.balance);
        assertEquals(800.0, account.availableBalance);
        verify(transferSuccessCounter).increment();
    }

    @Test
    void testUpdateBalance_accountNotActive() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.status = AccountStatus.BLOCKED;

        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.amount = 500.0;
        request.operation = BalanceOperation.DEPOSIT;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThrows(InvalidAccountOperationException.class,
                () -> balanceService.updateBalance(accountNumber, request));
        verify(transferFailureCounter).increment();
    }

    @Test
    void testUpdateBalance_accountNotFound() {
        String accountNumber = "9999999999999999";
        UpdateBalanceRequest request = new UpdateBalanceRequest();
        request.amount = 500.0;
        request.operation = BalanceOperation.DEPOSIT;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> balanceService.updateBalance(accountNumber, request));
    }

    @Test
    void testReserveFunds_success() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = 10000.0;
        account.availableBalance = 10000.0;
        account.status = AccountStatus.ACTIVE;

        ReserveFundsRequest request = new ReserveFundsRequest();
        request.amount = 500.0;
        request.description = "Pending transfer";
        request.expirationMinutes = 60;

        ReservationResponse expectedResponse = new ReservationResponse();
        expectedResponse.accountNumber = accountNumber;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountMapper.toReservationResponse(any(FundReservation.class))).thenReturn(expectedResponse);

        ReservationResponse result = balanceService.reserveFunds(accountNumber, request);

        assertEquals(accountNumber, result.accountNumber);
        assertEquals(9500.0, account.availableBalance);
        verify(fundReservationRepository).persist(any(FundReservation.class));
        verify(accountRepository).persist(account);
    }

    @Test
    void testReserveFunds_defaultExpiration() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = 10000.0;
        account.availableBalance = 10000.0;
        account.status = AccountStatus.ACTIVE;

        ReserveFundsRequest request = new ReserveFundsRequest();
        request.amount = 500.0;
        // expirationMinutes is null, should default to 30

        ReservationResponse expectedResponse = new ReservationResponse();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountMapper.toReservationResponse(any(FundReservation.class))).thenReturn(expectedResponse);

        balanceService.reserveFunds(accountNumber, request);

        verify(fundReservationRepository).persist(any(FundReservation.class));
    }

    @Test
    void testReserveFunds_insufficientBalance() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = 100.0;
        account.availableBalance = 100.0;
        account.status = AccountStatus.ACTIVE;

        ReserveFundsRequest request = new ReserveFundsRequest();
        request.amount = 500.0;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class,
                () -> balanceService.reserveFunds(accountNumber, request));
    }

    @Test
    void testReserveFunds_accountNotActive() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.status = AccountStatus.BLOCKED;

        ReserveFundsRequest request = new ReserveFundsRequest();
        request.amount = 500.0;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        assertThrows(InvalidAccountOperationException.class,
                () -> balanceService.reserveFunds(accountNumber, request));
    }

    @Test
    void testReserveFunds_accountNotFound() {
        String accountNumber = "9999999999999999";
        ReserveFundsRequest request = new ReserveFundsRequest();
        request.amount = 500.0;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> balanceService.reserveFunds(accountNumber, request));
    }

    @Test
    void testReleaseReservedFunds_success() {
        String accountNumber = "1234567890123456";
        UUID reservationId = UUID.randomUUID();

        Account account = new Account();
        account.accountNumber = accountNumber;
        account.availableBalance = 9500.0;

        FundReservation reservation = new FundReservation();
        reservation.reservationId = reservationId;
        reservation.accountNumber = accountNumber;
        reservation.amount = 500.0;
        reservation.status = ReservationStatus.ACTIVE;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(fundReservationRepository.findByReservationIdAndAccountNumber(reservationId, accountNumber))
                .thenReturn(Optional.of(reservation));

        balanceService.releaseReservedFunds(accountNumber, reservationId);

        assertEquals(ReservationStatus.RELEASED, reservation.status);
        assertEquals(10000.0, account.availableBalance);
        verify(fundReservationRepository).persist(reservation);
        verify(accountRepository).persist(account);
    }

    @Test
    void testReleaseReservedFunds_accountNotFound() {
        String accountNumber = "9999999999999999";
        UUID reservationId = UUID.randomUUID();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> balanceService.releaseReservedFunds(accountNumber, reservationId));
    }

    @Test
    void testReleaseReservedFunds_reservationNotFound() {
        String accountNumber = "1234567890123456";
        UUID reservationId = UUID.randomUUID();

        Account account = new Account();
        account.accountNumber = accountNumber;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(fundReservationRepository.findByReservationIdAndAccountNumber(reservationId, accountNumber))
                .thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class,
                () -> balanceService.releaseReservedFunds(accountNumber, reservationId));
    }

    @Test
    void testReleaseReservedFunds_reservationNotActive() {
        String accountNumber = "1234567890123456";
        UUID reservationId = UUID.randomUUID();

        Account account = new Account();
        account.accountNumber = accountNumber;

        FundReservation reservation = new FundReservation();
        reservation.reservationId = reservationId;
        reservation.accountNumber = accountNumber;
        reservation.amount = 500.0;
        reservation.status = ReservationStatus.RELEASED;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(fundReservationRepository.findByReservationIdAndAccountNumber(reservationId, accountNumber))
                .thenReturn(Optional.of(reservation));

        assertThrows(InvalidAccountOperationException.class,
                () -> balanceService.releaseReservedFunds(accountNumber, reservationId));
    }
}
