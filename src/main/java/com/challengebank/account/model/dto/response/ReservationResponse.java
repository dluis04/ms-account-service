package com.challengebank.account.model.dto.response;

import com.challengebank.account.model.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReservationResponse {

    public UUID reservationId;
    public String accountNumber;
    public double amount;
    public LocalDateTime expiresAt;
    public ReservationStatus status;
}
