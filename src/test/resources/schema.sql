-- Testcontainers init script
-- No CREATE DATABASE / CREATE USER / GRANT here.

CREATE SCHEMA IF NOT EXISTS securetx;

CREATE TABLE securetx.users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(150) NOT NULL UNIQUE,
    password_hash   VARCHAR(200) NOT NULL,
    role            VARCHAR(50)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE securetx.accounts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    currency        VARCHAR(3)    NOT NULL,
    balance         NUMERIC(19,4) NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

ALTER TABLE securetx.accounts
    ADD CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id) REFERENCES securetx.users(id);

ALTER TABLE securetx.accounts
    ADD CONSTRAINT chk_accounts_status
        CHECK (status IN ('ACTIVE', 'FROZEN'));

CREATE INDEX IF NOT EXISTS idx_accounts_user_id
    ON securetx.accounts (user_id);

CREATE TABLE securetx.transactions (
    id              BIGSERIAL PRIMARY KEY,
    external_id     VARCHAR(100) NOT NULL,
    from_account_id BIGINT       NOT NULL,
    to_account_id   BIGINT       NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    currency        VARCHAR(3)    NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    request_hash    VARCHAR(64)   NOT NULL,
    failure_reason  VARCHAR(250),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX ux_transactions_external_id
    ON securetx.transactions (external_id);

ALTER TABLE securetx.transactions
    ADD CONSTRAINT fk_tx_from_account
        FOREIGN KEY (from_account_id) REFERENCES securetx.accounts(id);

ALTER TABLE securetx.transactions
    ADD CONSTRAINT fk_tx_to_account
        FOREIGN KEY (to_account_id) REFERENCES securetx.accounts(id);

ALTER TABLE securetx.transactions
    ADD CONSTRAINT chk_transactions_amount_positive
        CHECK (amount > 0);

ALTER TABLE securetx.transactions
    ADD CONSTRAINT chk_transactions_status
        CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'));

CREATE INDEX IF NOT EXISTS idx_tx_from_account
    ON securetx.transactions (from_account_id);

CREATE INDEX IF NOT EXISTS idx_tx_to_account
    ON securetx.transactions (to_account_id);

CREATE INDEX IF NOT EXISTS idx_tx_created_at
    ON securetx.transactions (created_at);

CREATE TABLE securetx.ledger_entries (
    id              BIGSERIAL PRIMARY KEY,
    transaction_id  BIGINT       NOT NULL,
    account_id      BIGINT       NOT NULL,
    entry_type      VARCHAR(10)  NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    balance_after   NUMERIC(19,4) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

ALTER TABLE securetx.ledger_entries
    ADD CONSTRAINT fk_ledger_tx
        FOREIGN KEY (transaction_id) REFERENCES securetx.transactions(id);

ALTER TABLE securetx.ledger_entries
    ADD CONSTRAINT fk_ledger_account
        FOREIGN KEY (account_id) REFERENCES securetx.accounts(id);

ALTER TABLE securetx.ledger_entries
    ADD CONSTRAINT chk_ledger_entry_type
        CHECK (entry_type IN ('DEBIT', 'CREDIT'));

CREATE INDEX IF NOT EXISTS idx_ledger_account
    ON securetx.ledger_entries (account_id);

CREATE INDEX IF NOT EXISTS idx_ledger_transaction
    ON securetx.ledger_entries (transaction_id);

CREATE TABLE securetx.audit_logs (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT,
    action           VARCHAR(100) NOT NULL,
    request_metadata JSONB,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

ALTER TABLE securetx.audit_logs
    ADD CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id) REFERENCES securetx.users(id);

CREATE INDEX IF NOT EXISTS idx_audit_user_created_at
    ON securetx.audit_logs (user_id, created_at);
