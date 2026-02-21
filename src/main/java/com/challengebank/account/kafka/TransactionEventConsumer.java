package com.challengebank.account.kafka;

import com.challengebank.account.kafka.event.TransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class TransactionEventConsumer {

    @Inject
    ObjectMapper objectMapper;

    @Incoming("transactions-completed")
    public void onTransactionCompleted(String message) {
        try {
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);
            Log.infof("[AUDIT] Transaction COMPLETED - ID: %s, Type: %s, Source: %s, Dest: %s, Amount: %.2f %s, By: %s",
                    event.transactionId,
                    event.transactionType,
                    event.sourceAccountNumber,
                    event.destinationAccountNumber,
                    event.amount,
                    event.currency,
                    event.initiatedBy);
        } catch (Exception e) {
            Log.errorf("Failed to process COMPLETED transaction event: %s", e.getMessage());
        }
    }

    @Incoming("transactions-failed")
    public void onTransactionFailed(String message) {
        try {
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);
            Log.warnf("[AUDIT] Transaction FAILED - ID: %s, Type: %s, Source: %s, Dest: %s, Amount: %.2f %s, Error: %s",
                    event.transactionId,
                    event.transactionType,
                    event.sourceAccountNumber,
                    event.destinationAccountNumber,
                    event.amount,
                    event.currency,
                    event.errorMessage);
        } catch (Exception e) {
            Log.errorf("Failed to process FAILED transaction event: %s", e.getMessage());
        }
    }
}
