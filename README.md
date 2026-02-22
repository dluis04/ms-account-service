# MS Account Service

Microservice for bank account management for Challenge Bank. Handles customer accounts, balances, fund reservations, and availability validations.

## Architecture

```
ms-account-service/
├── src/main/java/com/challengebank/account/
│   ├── AccountApplication.java            # Application entry point
│   ├── config/
│   │   └── MetricsConfig.java             # Custom Prometheus metrics
│   ├── controller/
│   │   ├── AccountController.java         # Account CRUD
│   │   ├── BalanceController.java         # Balance and reservation operations
│   │   └── ValidationController.java      # Fund and account validation
│   ├── exception/
│   │   ├── AccountNotFoundException.java
│   │   ├── InsufficientFundsException.java
│   │   ├── InvalidAccountOperationException.java
│   │   ├── ReservationNotFoundException.java
│   │   └── GlobalExceptionHandler.java    # Centralized ExceptionMappers
│   ├── health/
│   │   └── DatabaseHealthCheck.java       # Database health check
│   ├── kafka/
│   │   ├── TransactionEventConsumer.java  # Kafka consumer for transaction events
│   │   └── event/
│   │       └── TransactionEvent.java      # Transaction event model
│   ├── logging/
│   │   └── CorrelationIdFilter.java       # Correlation ID in requests
│   ├── mapper/
│   │   └── AccountMapper.java             # Entity <-> DTO mapping
│   ├── model/
│   │   ├── dto/request/                   # Request DTOs with Bean Validation
│   │   ├── dto/response/                  # Response DTOs
│   │   ├── entity/                        # JPA Entities (Panache)
│   │   └── enums/                         # AccountStatus, AccountType, etc.
│   ├── repository/
│   │   ├── AccountRepository.java         # Panache Repository
│   │   └── FundReservationRepository.java # Fund reservations
│   └── service/
│       ├── AccountService.java            # Account business logic
│       ├── BalanceService.java            # Balance and reservation logic
│       └── ValidationService.java         # Fund/account validations
├── src/main/resources/
│   ├── application.properties             # Main configuration
│   ├── db/migration/                      # Flyway migrations
│   ├── publicKey.pem                      # JWT public key (not tracked in git)
│   └── privateKey.pem                     # JWT private key (not tracked in git)
└── src/test/                              # Unit and integration tests
```

### MVC Pattern

- **Model**: JPA entities with Hibernate ORM Panache (`Account`, `FundReservation`)
- **View**: Request/response DTOs with Bean Validation
- **Controller**: REST endpoints with RestEasy Reactive

### Tech Stack

| Technology | Version | Usage |
|---|---|---|
| Java | 21 | Language |
| Quarkus | 3.15 LTS | Framework |
| Maven | 3.9+ | Build |
| PostgreSQL | 16 | Database |
| Hibernate ORM Panache | - | Persistence |
| SmallRye JWT | - | Authentication |
| SmallRye Reactive Messaging | - | Kafka integration |
| Apache Kafka | - | Event streaming |
| Micrometer + Prometheus | - | Metrics |
| Flyway | - | DB Migrations |
| JUnit 5 + Mockito | - | Testing |
| Docker | - | Containers |

## Kafka Integration

This service acts as a **Kafka consumer**. It listens for transaction events published by `ms-transaction-service` and records them in the audit log.

### Topics

| Channel | Topic | Description |
|---|---|---|
| `transactions-completed` | `transactions.completed` | Successful transactions |
| `transactions-failed` | `transactions.failed` | Failed transactions |

### Consumer Group

```
account-service-group
```

### Consumer Implementation

**File:** `src/main/java/com/challengebank/account/kafka/TransactionEventConsumer.java`

| Method | Channel | Action |
|---|---|---|
| `onTransactionCompleted()` | `transactions-completed` | Logs `[AUDIT]` with transaction details (ID, type, accounts, amount, currency, initiated by) |
| `onTransactionFailed()` | `transactions-failed` | Logs `[AUDIT]` warning with error message |

### TransactionEvent Model

| Field | Type | Description |
|---|---|---|
| `eventId` | `String` | Unique event identifier |
| `eventType` | `String` | Event type |
| `transactionId` | `String` | Transaction identifier |
| `transactionType` | `String` | Type of transaction (TRANSFER, DEPOSIT, etc.) |
| `sourceAccountNumber` | `String` | Origin account |
| `destinationAccountNumber` | `String` | Destination account |
| `amount` | `double` | Transaction amount |
| `fee` | `double` | Applied fee |
| `totalAmount` | `double` | Total including fee |
| `currency` | `String` | Currency (USD, etc.) |
| `status` | `String` | Transaction status |
| `referenceNumber` | `String` | External reference number |
| `initiatedBy` | `String` | User who initiated the transaction |
| `errorMessage` | `String` | Error message (only for failed events) |
| `timestamp` | `LocalDateTime` | Event timestamp |

### Kafka Configuration

```properties
kafka.bootstrap.servers=localhost:9092

mp.messaging.incoming.transactions-completed.connector=smallrye-kafka
mp.messaging.incoming.transactions-completed.topic=transactions.completed
mp.messaging.incoming.transactions-completed.group.id=account-service-group
mp.messaging.incoming.transactions-completed.auto.offset.reset=earliest

mp.messaging.incoming.transactions-failed.connector=smallrye-kafka
mp.messaging.incoming.transactions-failed.topic=transactions.failed
mp.messaging.incoming.transactions-failed.group.id=account-service-group
mp.messaging.incoming.transactions-failed.auto.offset.reset=earliest
```

### Event Flow

```
ms-transaction-service
        │
        ├──► transactions.completed ──► onTransactionCompleted() ──► [AUDIT] INFO log
        │
        └──► transactions.failed    ──► onTransactionFailed()    ──► [AUDIT] WARN log
```

> **Note:** Kafka consumers run asynchronously in the background and do not affect REST API responses. Tests use `smallrye-in-memory` connector instead of a real Kafka instance.

## Connection with ms-customer-service

This microservice connects to `ms-customer-service` via REST Client configured in `application.properties`:

```properties
quarkus.rest-client.customer-service.url=http://localhost:8080
```

The relationship is through the `customerId` (UUID) field present in the `Account` entity, which references the `customerId` from the customer microservice.

## Running the Service

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker and Docker Compose

### With Docker Compose

```bash
cd ms-account-service
docker-compose up -d
```

This starts PostgreSQL on port `5433` and the service on port `8081`.

### Local Development

1. Start only the database:
```bash
docker-compose up -d postgres
```

2. Run in development mode:
```bash
mvn quarkus:dev
```

### Run Tests

```bash
mvn test
```

### Run Tests with Coverage Report

```bash
mvn verify
```

The JaCoCo report is generated at `target/jacoco-report/`.

## Endpoints

### Accounts (`/v1/accounts`)

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| `GET` | `/v1/accounts` | List accounts (paginated) | ROLE_USER, ROLE_ADMIN |
| `POST` | `/v1/accounts` | Create account | ROLE_ADMIN |
| `GET` | `/v1/accounts/{accountNumber}` | Get account by number | ROLE_USER, ROLE_ADMIN |
| `PUT` | `/v1/accounts/{accountNumber}` | Update account | ROLE_ADMIN |
| `DELETE` | `/v1/accounts/{accountNumber}` | Close account (soft delete) | ROLE_ADMIN |
| `GET` | `/v1/accounts/customer/{customerId}` | Accounts by customer | ROLE_USER, ROLE_ADMIN |
| `PATCH` | `/v1/accounts/{accountNumber}/status` | Update status | ROLE_ADMIN |

### Balances (`/v1/accounts/{accountNumber}/balance`)

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| `GET` | `/v1/accounts/{accountNumber}/balance` | Get balance | ROLE_USER, ROLE_ADMIN |
| `POST` | `/v1/accounts/{accountNumber}/balance/update` | Update balance | ROLE_ADMIN |
| `POST` | `/v1/accounts/{accountNumber}/reserve` | Reserve funds | ROLE_USER, ROLE_ADMIN |
| `POST` | `/v1/accounts/{accountNumber}/reserve/{reservationId}/release` | Release reservation | ROLE_USER, ROLE_ADMIN |

### Validations

| Method | Endpoint | Description | Roles |
|---|---|---|---|
| `POST` | `/v1/accounts/validate/funds` | Validate fund availability | ROLE_USER, ROLE_ADMIN |
| `GET` | `/v1/accounts/{accountNumber}/validate` | Validate account exists and is active | ROLE_USER, ROLE_ADMIN |

### Observability

| Endpoint | Description |
|---|---|
| `/q/health` | Health checks |
| `/q/health/ready` | Readiness (includes DB) |
| `/q/health/live` | Liveness |
| `/q/metrics` | Prometheus metrics |

## Usage Examples

### Get JWT Token (using the included PEM keys)

```bash
# Test credentials are validated via JWT.
# Generate token with SmallRye JWT (see Credentials section)
```

### Create Account

```bash
curl -X POST http://localhost:8081/v1/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -d '{
    "customerId": "550e8400-e29b-41d4-a716-446655440000",
    "accountType": "CHECKING",
    "initialDeposit": 1000.00,
    "currency": "USD",
    "overdraftLimit": 500.00
  }'
```

### List Accounts with Filters

```bash
curl http://localhost:8081/v1/accounts?status=ACTIVE&page=0&size=10 \
  -H "Authorization: Bearer <TOKEN>"
```

### Get Balance

```bash
curl http://localhost:8081/v1/accounts/1234567890123456/balance \
  -H "Authorization: Bearer <TOKEN>"
```

### Deposit Funds

```bash
curl -X POST http://localhost:8081/v1/accounts/1234567890123456/balance/update \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -d '{
    "amount": 500.00,
    "operation": "DEPOSIT",
    "description": "ATM deposit",
    "referenceId": "TXN-12345"
  }'
```

### Withdraw Funds

```bash
curl -X POST http://localhost:8081/v1/accounts/1234567890123456/balance/update \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -d '{
    "amount": 200.00,
    "operation": "WITHDRAWAL",
    "description": "ATM withdrawal"
  }'
```

### Reserve Funds

```bash
curl -X POST http://localhost:8081/v1/accounts/1234567890123456/reserve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "amount": 500.00,
    "description": "Pending transfer to account 9876543210",
    "expirationMinutes": 30
  }'
```

### Release Reservation

```bash
curl -X POST http://localhost:8081/v1/accounts/1234567890123456/reserve/3fa85f64-5717-4562-b3fc-2c963f66afa6/release \
  -H "Authorization: Bearer <TOKEN>"
```

### Validate Available Funds

```bash
curl -X POST http://localhost:8081/v1/accounts/validate/funds \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "accountNumber": "1234567890123456",
    "amount": 1500.00,
    "includeOverdraft": false
  }'
```

### Validate Account

```bash
curl http://localhost:8081/v1/accounts/1234567890123456/validate \
  -H "Authorization: Bearer <TOKEN>"
```

### Update Account Status

```bash
curl -X PATCH http://localhost:8081/v1/accounts/1234567890123456/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -d '{
    "status": "BLOCKED",
    "reason": "Suspicious activity detected"
  }'
```

### Close Account

```bash
curl -X DELETE http://localhost:8081/v1/accounts/1234567890123456 \
  -H "Authorization: Bearer <TOKEN_ADMIN>"
```

## Test Credentials

Authentication uses JWT with the RSA keys included in `src/main/resources/`.

| User | Role | Description |
|---|---|---|
| `admin1` | `ROLE_ADMIN` | Full access (create, update, delete accounts, manage balances) |
| `user1` | `ROLE_USER` | Read accounts, balances and validations |

### Generate Test JWT Token

Tokens are generated using SmallRye JWT with the project's PEM keys. Tests use `@TestSecurity` to simulate authentication.

**Issuer**: `https://challengebank.com`

### JWT Configuration

```properties
mp.jwt.verify.publickey.location=publicKey.pem
mp.jwt.verify.issuer=https://challengebank.com
smallrye.jwt.sign.key.location=privateKey.pem
```

## Custom Metrics

| Metric | Type | Description |
|---|---|---|
| `account.operations.success` | Counter | Successful account operations |
| `account.operations.failure` | Counter | Failed account operations |
| `account.transfer.success` | Counter | Successful transfers/balance operations |
| `account.transfer.failure` | Counter | Failed transfers/balance operations |
| `account.transfer.total.amount` | Gauge | Total amount transferred |
| `account.validation.success` | Counter | Successful validations |
| `account.validation.failure` | Counter | Failed validations |
| `account.active.total` | Gauge | Total active accounts |

## Database

- **Database**: `account_db`
- **Port**: `5433` (different from customer service at `5432`)
- **User**: `account_user`
- **Password**: `account_pass`

### Tables

- `accounts` - Bank accounts
- `fund_reservations` - Temporary fund reservations