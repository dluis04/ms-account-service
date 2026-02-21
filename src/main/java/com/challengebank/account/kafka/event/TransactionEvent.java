package com.challengebank.account.kafka.event;

import java.time.LocalDateTime;

public class TransactionEvent {

    public String eventId;
    public String eventType;
    public String transactionId;
    public String transactionType;
    public String sourceAccountNumber;
    public String destinationAccountNumber;
    public double amount;
    public double fee;
    public double totalAmount;
    public String currency;
    public String status;
    public String referenceNumber;
    public String initiatedBy;
    public String errorMessage;
    public LocalDateTime timestamp;
}
