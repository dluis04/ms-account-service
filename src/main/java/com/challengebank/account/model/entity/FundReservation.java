package com.challengebank.account.model.entity;

import com.challengebank.account.model.enums.ReservationStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fund_reservations")
public class FundReservation extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reservation_id", updatable = false, nullable = false)
    public UUID reservationId;

    @Column(name = "account_number", nullable = false, length = 20)
    public String accountNumber;

    @Column(name = "amount", nullable = false)
    public double amount;

    @Column(name = "description", length = 500)
    public String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    public ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    public LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    public LocalDateTime createdAt;
}
