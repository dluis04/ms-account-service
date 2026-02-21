CREATE TABLE accounts (
    account_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number       VARCHAR(20)    NOT NULL,
    customer_id          UUID           NOT NULL,
    account_type         VARCHAR(10)    NOT NULL,
    balance              DOUBLE PRECISION NOT NULL DEFAULT 0,
    available_balance    DOUBLE PRECISION NOT NULL DEFAULT 0,
    currency             VARCHAR(3)     NOT NULL DEFAULT 'USD',
    status               VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE',
    overdraft_limit      DOUBLE PRECISION DEFAULT 0,
    interest_rate        DOUBLE PRECISION DEFAULT 0,
    last_transaction_date TIMESTAMP,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP,

    CONSTRAINT uk_accounts_account_number UNIQUE (account_number),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT chk_accounts_type CHECK (account_type IN ('CHECKING', 'SAVINGS', 'BUSINESS', 'PREMIUM'))
);

CREATE TABLE fund_reservations (
    reservation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number VARCHAR(20)      NOT NULL,
    amount         DOUBLE PRECISION NOT NULL,
    description    VARCHAR(500),
    status         VARCHAR(10)      NOT NULL DEFAULT 'ACTIVE',
    expires_at     TIMESTAMP        NOT NULL,
    created_at     TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservations_account FOREIGN KEY (account_number)
        REFERENCES accounts (account_number),
    CONSTRAINT chk_reservation_status CHECK (status IN ('ACTIVE', 'RELEASED', 'EXPIRED'))
);

CREATE INDEX idx_accounts_customer_id ON accounts (customer_id);
CREATE INDEX idx_accounts_status ON accounts (status);
CREATE INDEX idx_accounts_account_number ON accounts (account_number);
CREATE INDEX idx_reservations_account_number ON fund_reservations (account_number);
CREATE INDEX idx_reservations_status ON fund_reservations (status);
