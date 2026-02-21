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
import io.micrometer.core.instrument.Gauge;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class BalanceService {

    @Inject
    AccountRepository accountRepository;

    @Inject
    FundReservationRepository fundReservationRepository;

    @Inject
    AccountMapper accountMapper;

    @Inject
    MeterRegistry meterRegistry;

    Counter transferSuccessCounter;
    Counter transferFailureCounter;
    final AtomicReference<Double> totalTransferred = new AtomicReference<>(0.0);

    @PostConstruct
    void initMetrics() {
        transferSuccessCounter = meterRegistry.counter("account.transfer.success");
        transferFailureCounter = meterRegistry.counter("account.transfer.failure");
        Gauge.builder("account.transfer.total.amount", totalTransferred, AtomicReference::get)
                .description("Total amount transferred")
                .register(meterRegistry);
    }

    public BalanceResponse getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        double reservedAmount = fundReservationRepository.sumActiveReservations(accountNumber);
        return accountMapper.toBalanceResponse(account, reservedAmount);
    }

    @Transactional
    public BalanceResponse updateBalance(String accountNumber, UpdateBalanceRequest request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (account.status != AccountStatus.ACTIVE) {
            transferFailureCounter.increment();
            throw new InvalidAccountOperationException("Account is not active: " + accountNumber);
        }

        double amount = request.amount;
        if (request.operation == BalanceOperation.WITHDRAWAL) {
            if (amount > account.availableBalance + account.overdraftLimit) {
                transferFailureCounter.increment();
                throw new InsufficientFundsException("Insufficient funds for withdrawal of " + amount);
            }
            account.balance -= amount;
            account.availableBalance -= amount;
        } else if (request.operation == BalanceOperation.DEPOSIT) {
            account.balance += amount;
            account.availableBalance += amount;
        } else {
            account.balance += amount;
            account.availableBalance += amount;
        }

        account.lastTransactionDate = LocalDateTime.now();
        accountRepository.persist(account);
        transferSuccessCounter.increment();
        totalTransferred.updateAndGet(v -> v + Math.abs(amount));
        Log.infof("Balance updated for account %s: %s %s", accountNumber, request.operation, amount);

        double reservedAmount = fundReservationRepository.sumActiveReservations(accountNumber);
        return accountMapper.toBalanceResponse(account, reservedAmount);
    }

    @Transactional
    public ReservationResponse reserveFunds(String accountNumber, ReserveFundsRequest request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (account.status != AccountStatus.ACTIVE) {
            throw new InvalidAccountOperationException("Account is not active: " + accountNumber);
        }

        if (request.amount > account.availableBalance) {
            throw new InsufficientFundsException("Insufficient available balance for reservation of " + request.amount);
        }

        int expirationMinutes = request.expirationMinutes != null ? request.expirationMinutes : 30;

        FundReservation reservation = new FundReservation();
        reservation.accountNumber = accountNumber;
        reservation.amount = request.amount;
        reservation.description = request.description;
        reservation.status = ReservationStatus.ACTIVE;
        reservation.expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);

        fundReservationRepository.persist(reservation);

        account.availableBalance -= request.amount;
        accountRepository.persist(account);

        Log.infof("Funds reserved: %s on account %s", request.amount, accountNumber);
        return accountMapper.toReservationResponse(reservation);
    }

    @Transactional
    public void releaseReservedFunds(String accountNumber, UUID reservationId) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        FundReservation reservation = fundReservationRepository
                .findByReservationIdAndAccountNumber(reservationId, accountNumber)
                .orElseThrow(() -> new ReservationNotFoundException(
                        "Reservation not found: " + reservationId + " for account: " + accountNumber));

        if (reservation.status != ReservationStatus.ACTIVE) {
            throw new InvalidAccountOperationException("Reservation is not active: " + reservationId);
        }

        reservation.status = ReservationStatus.RELEASED;
        fundReservationRepository.persist(reservation);

        account.availableBalance += reservation.amount;
        accountRepository.persist(account);

        Log.infof("Funds released: %s on account %s (reservation: %s)", reservation.amount, accountNumber, reservationId);
    }
}
