package com.challengebank.account.repository;

import com.challengebank.account.model.entity.FundReservation;
import com.challengebank.account.model.enums.ReservationStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class FundReservationRepository implements PanacheRepositoryBase<FundReservation, UUID> {

    public List<FundReservation> findActiveByAccountNumber(String accountNumber) {
        return find("accountNumber = ?1 and status = ?2", accountNumber, ReservationStatus.ACTIVE).list();
    }

    public Optional<FundReservation> findByReservationIdAndAccountNumber(UUID reservationId, String accountNumber) {
        return find("reservationId = ?1 and accountNumber = ?2", reservationId, accountNumber).firstResultOptional();
    }

    public double sumActiveReservations(String accountNumber) {
        List<FundReservation> active = findActiveByAccountNumber(accountNumber);
        return active.stream().mapToDouble(r -> r.amount).sum();
    }
}
