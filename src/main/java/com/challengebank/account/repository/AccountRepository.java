package com.challengebank.account.repository;

import com.challengebank.account.model.entity.Account;
import com.challengebank.account.model.enums.AccountStatus;
import com.challengebank.account.model.enums.AccountType;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AccountRepository implements PanacheRepositoryBase<Account, UUID> {

    public Optional<Account> findByAccountNumber(String accountNumber) {
        return find("accountNumber", accountNumber).firstResultOptional();
    }

    public List<Account> findByCustomerId(UUID customerId) {
        return find("customerId", customerId).list();
    }

    public PanacheQuery<Account> findByStatus(AccountStatus status) {
        return find("status", status);
    }

    public PanacheQuery<Account> findByAccountType(AccountType accountType) {
        return find("accountType", accountType);
    }

    public PanacheQuery<Account> findByStatusAndAccountType(AccountStatus status, AccountType accountType) {
        return find("status = ?1 and accountType = ?2", status, accountType);
    }

    public boolean existsByAccountNumber(String accountNumber) {
        return count("accountNumber", accountNumber) > 0;
    }

    public long countByStatus(AccountStatus status) {
        return count("status", status);
    }
}
