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
import io.quarkus.panache.common.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    AccountMapper accountMapper;

    @Mock
    MeterRegistry meterRegistry;

    @Mock
    Counter successCounter;

    @Mock
    Counter failureCounter;

    @InjectMocks
    AccountService accountService;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter("account.operations.success")).thenReturn(successCounter);
        when(meterRegistry.counter("account.operations.failure")).thenReturn(failureCounter);
        accountService.initMetrics();
    }

    @Test
    void testCreateAccount_success() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.customerId = UUID.randomUUID();
        request.accountType = AccountType.CHECKING;
        request.initialDeposit = 1000.0;

        Account account = new Account();
        account.accountId = UUID.randomUUID();
        account.accountNumber = "1234567890123456";

        AccountResponse expectedResponse = new AccountResponse();
        expectedResponse.accountId = account.accountId;

        when(accountMapper.toEntity(request)).thenReturn(account);
        when(accountRepository.existsByAccountNumber("1234567890123456")).thenReturn(false);
        when(accountMapper.toResponse(account)).thenReturn(expectedResponse);

        AccountResponse result = accountService.createAccount(request);

        assertEquals(expectedResponse.accountId, result.accountId);
        verify(accountRepository).persist(account);
        verify(successCounter).increment();
    }

    @Test
    void testCreateAccount_duplicateAccountNumber_regenerates() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.customerId = UUID.randomUUID();
        request.accountType = AccountType.CHECKING;
        request.initialDeposit = 1000.0;

        Account account = new Account();
        account.accountId = UUID.randomUUID();
        account.accountNumber = "1234567890123456";

        AccountResponse expectedResponse = new AccountResponse();
        expectedResponse.accountId = account.accountId;

        when(accountMapper.toEntity(request)).thenReturn(account);
        when(accountRepository.existsByAccountNumber("1234567890123456")).thenReturn(true);
        when(accountMapper.generateAccountNumber()).thenReturn("9876543210987654");
        when(accountRepository.existsByAccountNumber("9876543210987654")).thenReturn(false);
        when(accountMapper.toResponse(account)).thenReturn(expectedResponse);

        AccountResponse result = accountService.createAccount(request);

        assertEquals("9876543210987654", account.accountNumber);
        verify(accountRepository).persist(account);
        verify(successCounter).increment();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetAllAccounts_noFilter() {
        PanacheQuery<Account> query = mock(PanacheQuery.class);
        Account account = new Account();
        account.accountId = UUID.randomUUID();
        List<Account> accounts = List.of(account);
        AccountPageResponse expectedPage = new AccountPageResponse();

        when(accountRepository.findAll()).thenReturn(query);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(accounts);
        when(query.count()).thenReturn(1L);
        when(accountMapper.toPageResponse(accounts, 0, 10, 1L)).thenReturn(expectedPage);

        AccountPageResponse result = accountService.getAllAccounts(0, 10, null, null);

        assertSame(expectedPage, result);
        verify(accountRepository).findAll();
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetAllAccounts_withStatusFilter() {
        PanacheQuery<Account> query = mock(PanacheQuery.class);
        List<Account> accounts = List.of(new Account());
        AccountPageResponse expectedPage = new AccountPageResponse();

        when(accountRepository.findByStatus(AccountStatus.ACTIVE)).thenReturn(query);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(accounts);
        when(query.count()).thenReturn(5L);
        when(accountMapper.toPageResponse(accounts, 0, 10, 5L)).thenReturn(expectedPage);

        AccountPageResponse result = accountService.getAllAccounts(0, 10, AccountStatus.ACTIVE, null);

        assertSame(expectedPage, result);
        verify(accountRepository).findByStatus(AccountStatus.ACTIVE);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetAllAccounts_withAccountTypeFilter() {
        PanacheQuery<Account> query = mock(PanacheQuery.class);
        List<Account> accounts = List.of(new Account());
        AccountPageResponse expectedPage = new AccountPageResponse();

        when(accountRepository.findByAccountType(AccountType.SAVINGS)).thenReturn(query);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(accounts);
        when(query.count()).thenReturn(3L);
        when(accountMapper.toPageResponse(accounts, 0, 10, 3L)).thenReturn(expectedPage);

        AccountPageResponse result = accountService.getAllAccounts(0, 10, null, AccountType.SAVINGS);

        assertSame(expectedPage, result);
        verify(accountRepository).findByAccountType(AccountType.SAVINGS);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetAllAccounts_withBothFilters() {
        PanacheQuery<Account> query = mock(PanacheQuery.class);
        List<Account> accounts = List.of(new Account());
        AccountPageResponse expectedPage = new AccountPageResponse();

        when(accountRepository.findByStatusAndAccountType(AccountStatus.ACTIVE, AccountType.CHECKING))
                .thenReturn(query);
        when(query.page(any(Page.class))).thenReturn(query);
        when(query.list()).thenReturn(accounts);
        when(query.count()).thenReturn(2L);
        when(accountMapper.toPageResponse(accounts, 0, 10, 2L)).thenReturn(expectedPage);

        AccountPageResponse result = accountService.getAllAccounts(0, 10, AccountStatus.ACTIVE, AccountType.CHECKING);

        assertSame(expectedPage, result);
        verify(accountRepository).findByStatusAndAccountType(AccountStatus.ACTIVE, AccountType.CHECKING);
    }

    @Test
    void testGetAccountByNumber_found() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        AccountResponse expectedResponse = new AccountResponse();
        expectedResponse.accountNumber = accountNumber;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(expectedResponse);

        AccountResponse result = accountService.getAccountByNumber(accountNumber);

        assertEquals(accountNumber, result.accountNumber);
    }

    @Test
    void testGetAccountByNumber_notFound() {
        String accountNumber = "9999999999999999";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccountByNumber(accountNumber));
        assertTrue(ex.getMessage().contains(accountNumber));
    }

    @Test
    void testGetAccountsByCustomerId() {
        UUID customerId = UUID.randomUUID();
        Account a1 = new Account();
        a1.accountNumber = "1111111111111111";
        Account a2 = new Account();
        a2.accountNumber = "2222222222222222";

        AccountResponse r1 = new AccountResponse();
        r1.accountNumber = "1111111111111111";
        AccountResponse r2 = new AccountResponse();
        r2.accountNumber = "2222222222222222";

        when(accountRepository.findByCustomerId(customerId)).thenReturn(List.of(a1, a2));
        when(accountMapper.toResponse(a1)).thenReturn(r1);
        when(accountMapper.toResponse(a2)).thenReturn(r2);

        List<AccountResponse> result = accountService.getAccountsByCustomerId(customerId);

        assertEquals(2, result.size());
        assertEquals("1111111111111111", result.get(0).accountNumber);
        assertEquals("2222222222222222", result.get(1).accountNumber);
    }

    @Test
    void testUpdateAccount_success() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;

        UpdateAccountRequest request = new UpdateAccountRequest();
        request.overdraftLimit = 1000.0;

        AccountResponse expectedResponse = new AccountResponse();
        expectedResponse.accountNumber = accountNumber;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(expectedResponse);

        AccountResponse result = accountService.updateAccount(accountNumber, request);

        assertEquals(accountNumber, result.accountNumber);
        verify(accountMapper).updateEntity(account, request);
        verify(accountRepository).persist(account);
        verify(successCounter).increment();
    }

    @Test
    void testUpdateAccount_notFound() {
        String accountNumber = "9999999999999999";
        UpdateAccountRequest request = new UpdateAccountRequest();

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.updateAccount(accountNumber, request));
    }

    @Test
    void testCloseAccount_success() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = 0;
        account.status = AccountStatus.ACTIVE;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        accountService.closeAccount(accountNumber);

        assertEquals(AccountStatus.CLOSED, account.status);
        verify(accountRepository).persist(account);
        verify(successCounter).increment();
    }

    @Test
    void testCloseAccount_notFound() {
        String accountNumber = "9999999999999999";
        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.closeAccount(accountNumber));
    }

    @Test
    void testCloseAccount_nonZeroBalance() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.balance = 500.0;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        InvalidAccountOperationException ex = assertThrows(InvalidAccountOperationException.class,
                () -> accountService.closeAccount(accountNumber));
        assertTrue(ex.getMessage().contains("non-zero balance"));
        verify(failureCounter).increment();
        verify(accountRepository, never()).persist(any(Account.class));
    }

    @Test
    void testUpdateAccountStatus_success() {
        String accountNumber = "1234567890123456";
        Account account = new Account();
        account.accountNumber = accountNumber;
        account.status = AccountStatus.ACTIVE;

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.status = AccountStatus.BLOCKED;
        request.reason = "Suspicious activity";

        AccountResponse expectedResponse = new AccountResponse();
        expectedResponse.accountNumber = accountNumber;
        expectedResponse.status = AccountStatus.BLOCKED;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(expectedResponse);

        AccountResponse result = accountService.updateAccountStatus(accountNumber, request);

        assertEquals(AccountStatus.BLOCKED, account.status);
        assertEquals(AccountStatus.BLOCKED, result.status);
        verify(accountRepository).persist(account);
        verify(successCounter).increment();
    }

    @Test
    void testUpdateAccountStatus_notFound() {
        String accountNumber = "9999999999999999";
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.status = AccountStatus.BLOCKED;

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.updateAccountStatus(accountNumber, request));
    }
}
