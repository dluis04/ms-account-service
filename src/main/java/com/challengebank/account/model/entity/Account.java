package com.challengebank.account.model.entity;

import com.challengebank.account.model.enums.AccountStatus;
import com.challengebank.account.model.enums.AccountType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_accounts_account_number", columnNames = {"account_number"})
})
public class Account extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_id", updatable = false, nullable = false)
    public UUID accountId;

    @Column(name = "account_number", nullable = false, length = 20)
    public String accountNumber;

    @Column(name = "customer_id", nullable = false)
    public UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10)
    public AccountType accountType;

    @Column(name = "balance", nullable = false)
    public double balance;

    @Column(name = "available_balance", nullable = false)
    public double availableBalance;

    @Column(name = "currency", nullable = false, length = 3)
    public String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    public AccountStatus status;

    @Column(name = "overdraft_limit")
    public double overdraftLimit;

    @Column(name = "interest_rate")
    public double interestRate;

    @Column(name = "last_transaction_date")
    public LocalDateTime lastTransactionDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}
