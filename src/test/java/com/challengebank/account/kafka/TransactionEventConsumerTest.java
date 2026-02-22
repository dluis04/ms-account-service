package com.challengebank.account.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {

    @InjectMocks
    TransactionEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void testOnTransactionCompleted_validMessage() {
        String message = buildEventJson("TRANSACTION_COMPLETED", "COMPLETED", null);

        assertDoesNotThrow(() -> consumer.onTransactionCompleted(message));
    }

    @Test
    void testOnTransactionCompleted_transferEvent() {
        String message = buildTransferEventJson("TRANSACTION_COMPLETED", "COMPLETED");

        assertDoesNotThrow(() -> consumer.onTransactionCompleted(message));
    }

    @Test
    void testOnTransactionCompleted_invalidJson() {
        assertDoesNotThrow(() -> consumer.onTransactionCompleted("invalid-json"));
    }

    @Test
    void testOnTransactionCompleted_emptyMessage() {
        assertDoesNotThrow(() -> consumer.onTransactionCompleted(""));
    }

    @Test
    void testOnTransactionFailed_validMessage() {
        String message = buildEventJson("TRANSACTION_FAILED", "FAILED", "Insufficient funds");

        assertDoesNotThrow(() -> consumer.onTransactionFailed(message));
    }

    @Test
    void testOnTransactionFailed_invalidJson() {
        assertDoesNotThrow(() -> consumer.onTransactionFailed("not-valid-json"));
    }

    @Test
    void testOnTransactionFailed_nullErrorMessage() {
        String message = buildEventJson("TRANSACTION_FAILED", "FAILED", null);

        assertDoesNotThrow(() -> consumer.onTransactionFailed(message));
    }

    private String buildEventJson(String eventType, String status, String errorMessage) {
        String error = errorMessage != null ? "\"" + errorMessage + "\"" : "null";
        return String.format("""
                {
                    "eventId": "test-event-id",
                    "eventType": "%s",
                    "transactionId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
                    "transactionType": "DEPOSIT",
                    "sourceAccountNumber": null,
                    "destinationAccountNumber": "1234567890123456",
                    "amount": 500.00,
                    "fee": 0.00,
                    "totalAmount": 500.00,
                    "currency": "USD",
                    "status": "%s",
                    "referenceNumber": "DEP-001",
                    "initiatedBy": "user@bank.com",
                    "errorMessage": %s,
                    "timestamp": "2024-01-25T14:30:00"
                }
                """, eventType, status, error);
    }

    private String buildTransferEventJson(String eventType, String status) {
        return String.format("""
                {
                    "eventId": "test-event-id",
                    "eventType": "%s",
                    "transactionId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
                    "transactionType": "TRANSFER",
                    "sourceAccountNumber": "1111111111111111",
                    "destinationAccountNumber": "2222222222222222",
                    "amount": 1000.00,
                    "fee": 2.50,
                    "totalAmount": 1002.50,
                    "currency": "USD",
                    "status": "%s",
                    "referenceNumber": "REF-001",
                    "initiatedBy": "user@bank.com",
                    "errorMessage": null,
                    "timestamp": "2024-01-25T14:30:00"
                }
                """, eventType, status);
    }
}
