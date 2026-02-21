package com.challengebank.account.service;

import com.challengebank.account.exception.AccountNotFoundException;
import com.challengebank.account.exception.InvalidAccountOperationException;
import com.challengebank.account.mapper.AccountMapper;
import com.challengebank.account.model.dto.request.CreateAccountRequest;
import com.challengebank.account.model.dto.request.UpdateAccountRequest;
import com.challengebank.account.model.dto.request.UpdateStatusRequest;
import com.challengebank.account.model.dto.response.AccountPageResponse;
import com.challengebank.account.model.dto.response.AccountResponse;
import com.challengebank.account.model.entity.Account;
import com.challengebank.account.model.enums.AccountStatus;
import com.challengebank.account.model.enums.AccountType;
import com.challengebank.account.repository.AccountRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Page;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AccountService {

    @Inject
    AccountRepository accountRepository;

    @Inject
    AccountMapper accountMapper;

    @Inject
    MeterRegistry meterRegistry;

    Counter successCounter;
    Counter failureCounter;

    @PostConstruct
    void initMetrics() {
        successCounter = meterRegistry.counter("account.operations.success");
        failureCounter = meterRegistry.counter("account.operations.failure");
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = accountMapper.toEntity(request);

        while (accountRepository.existsByAccountNumber(account.accountNumber)) {
            account.accountNumber = accountMapper.generateAccountNumber();
        }

        accountRepository.persist(account);
        successCounter.increment();
        Log.infof("Account created: %s for customer: %s", account.accountNumber, account.customerId);
        return accountMapper.toResponse(account);
    }

    public AccountPageResponse getAllAccounts(int page, int size, AccountStatus status, AccountType accountType) {
        PanacheQuery<Account> query;
        if (status != null && accountType != null) {
            query = accountRepository.findByStatusAndAccountType(status, accountType);
        } else if (status != null) {
            query = accountRepository.findByStatus(status);
        } else if (accountType != null) {
            query = accountRepository.findByAccountType(accountType);
        } else {
            query = accountRepository.findAll();
        }
        List<Account> accounts = query.page(Page.of(page, size)).list();
        long total = query.count();
        return accountMapper.toPageResponse(accounts, page, size, total);
    }

    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return accountMapper.toResponse(account);
    }

    public List<AccountResponse> getAccountsByCustomerId(UUID customerId) {
        List<Account> accounts = accountRepository.findByCustomerId(customerId);
        return accounts.stream().map(accountMapper::toResponse).toList();
    }

    @Transactional
    public AccountResponse updateAccount(String accountNumber, UpdateAccountRequest request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        accountMapper.updateEntity(account, request);
        accountRepository.persist(account);
        successCounter.increment();
        Log.infof("Account updated: %s", accountNumber);
        return accountMapper.toResponse(account);
    }

    @Transactional
    public void closeAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (account.balance != 0) {
            failureCounter.increment();
            throw new InvalidAccountOperationException("Cannot close account with non-zero balance: " + account.balance);
        }

        account.status = AccountStatus.CLOSED;
        accountRepository.persist(account);
        successCounter.increment();
        Log.infof("Account closed: %s", accountNumber);
    }

    @Transactional
    public AccountResponse updateAccountStatus(String accountNumber, UpdateStatusRequest request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        account.status = request.status;
        accountRepository.persist(account);
        successCounter.increment();
        Log.infof("Account %s status updated to %s. Reason: %s", accountNumber, request.status, request.reason);
        return accountMapper.toResponse(account);
    }
}
