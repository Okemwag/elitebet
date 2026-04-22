CREATE TABLE elitebet.wallets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    principal_id varchar(160) NOT NULL,
    currency_code char(3) NOT NULL,
    status varchar(40) NOT NULL,
    balance_minor bigint NOT NULL DEFAULT 0,
    reserved_minor bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_wallets_principal_currency UNIQUE (principal_id, currency_code),
    CONSTRAINT uq_wallets_id_currency UNIQUE (id, currency_code),
    CONSTRAINT ck_wallet_status CHECK (status IN ('ACTIVE', 'LOCKED', 'CLOSED')),
    CONSTRAINT ck_wallet_currency_code CHECK (currency_code = upper(currency_code)),
    CONSTRAINT ck_wallet_balance_non_negative CHECK (balance_minor >= 0),
    CONSTRAINT ck_wallet_reserved_non_negative CHECK (reserved_minor >= 0),
    CONSTRAINT ck_wallet_reserved_not_above_balance CHECK (reserved_minor <= balance_minor)
);

CREATE INDEX idx_wallets_principal_id
    ON elitebet.wallets (principal_id);

CREATE TABLE elitebet.wallet_transactions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id uuid NOT NULL,
    transaction_type varchar(40) NOT NULL,
    status varchar(40) NOT NULL,
    amount_minor bigint NOT NULL,
    currency_code char(3) NOT NULL,
    idempotency_key varchar(160),
    reference_type varchar(80),
    reference_id varchar(160),
    external_reference varchar(160),
    failure_reason text,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    posted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_wallet_transactions_id_currency UNIQUE (id, currency_code),
    CONSTRAINT fk_wallet_transactions_wallet_currency FOREIGN KEY (wallet_id, currency_code)
        REFERENCES elitebet.wallets (id, currency_code),
    CONSTRAINT ck_wallet_transaction_type CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'BET_STAKE', 'PAYOUT', 'ADJUSTMENT', 'REVERSAL')),
    CONSTRAINT ck_wallet_transaction_status CHECK (status IN ('PENDING', 'POSTED', 'REVERSED', 'FAILED')),
    CONSTRAINT ck_wallet_transaction_amount_positive CHECK (amount_minor > 0),
    CONSTRAINT ck_wallet_transaction_currency_code CHECK (currency_code = upper(currency_code)),
    CONSTRAINT ck_wallet_transaction_posted_at CHECK ((status = 'POSTED' AND posted_at IS NOT NULL) OR (status <> 'POSTED'))
);

CREATE INDEX idx_wallet_transactions_wallet_created_at
    ON elitebet.wallet_transactions (wallet_id, created_at DESC);

CREATE INDEX idx_wallet_transactions_reference
    ON elitebet.wallet_transactions (reference_type, reference_id)
    WHERE reference_type IS NOT NULL AND reference_id IS NOT NULL;

CREATE UNIQUE INDEX uq_wallet_transactions_idempotency
    ON elitebet.wallet_transactions (wallet_id, transaction_type, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE elitebet.fund_reservations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id uuid NOT NULL,
    transaction_id uuid NOT NULL,
    amount_minor bigint NOT NULL,
    currency_code char(3) NOT NULL,
    status varchar(40) NOT NULL,
    idempotency_key varchar(160) NOT NULL,
    reference_type varchar(80) NOT NULL,
    reference_id varchar(160) NOT NULL,
    expires_at timestamptz NOT NULL,
    captured_at timestamptz,
    released_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_fund_reservations_id_currency UNIQUE (id, currency_code),
    CONSTRAINT fk_fund_reservations_wallet_currency FOREIGN KEY (wallet_id, currency_code)
        REFERENCES elitebet.wallets (id, currency_code),
    CONSTRAINT fk_fund_reservations_transaction_currency FOREIGN KEY (transaction_id, currency_code)
        REFERENCES elitebet.wallet_transactions (id, currency_code),
    CONSTRAINT uq_fund_reservations_wallet_idempotency UNIQUE (wallet_id, idempotency_key),
    CONSTRAINT uq_fund_reservations_reference UNIQUE (wallet_id, reference_type, reference_id),
    CONSTRAINT ck_fund_reservation_status CHECK (status IN ('HELD', 'RELEASED', 'CAPTURED', 'EXPIRED')),
    CONSTRAINT ck_fund_reservation_amount_positive CHECK (amount_minor > 0),
    CONSTRAINT ck_fund_reservation_currency_code CHECK (currency_code = upper(currency_code)),
    CONSTRAINT ck_fund_reservation_terminal_timestamp CHECK (
        (status = 'CAPTURED' AND captured_at IS NOT NULL AND released_at IS NULL)
        OR (status IN ('RELEASED', 'EXPIRED') AND released_at IS NOT NULL AND captured_at IS NULL)
        OR (status = 'HELD' AND captured_at IS NULL AND released_at IS NULL)
    )
);

CREATE INDEX idx_fund_reservations_wallet_status
    ON elitebet.fund_reservations (wallet_id, status, created_at DESC);

CREATE INDEX idx_fund_reservations_expiry
    ON elitebet.fund_reservations (status, expires_at)
    WHERE status = 'HELD';

CREATE TABLE elitebet.ledger_entries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id uuid NOT NULL,
    transaction_id uuid NOT NULL,
    reservation_id uuid,
    entry_type varchar(40) NOT NULL,
    amount_minor bigint NOT NULL,
    currency_code char(3) NOT NULL,
    balance_after_minor bigint NOT NULL,
    reference_type varchar(80),
    reference_id varchar(160),
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT fk_ledger_entries_wallet_currency FOREIGN KEY (wallet_id, currency_code)
        REFERENCES elitebet.wallets (id, currency_code),
    CONSTRAINT fk_ledger_entries_transaction_currency FOREIGN KEY (transaction_id, currency_code)
        REFERENCES elitebet.wallet_transactions (id, currency_code),
    CONSTRAINT fk_ledger_entries_reservation_currency FOREIGN KEY (reservation_id, currency_code)
        REFERENCES elitebet.fund_reservations (id, currency_code),
    CONSTRAINT ck_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_ledger_entry_amount_positive CHECK (amount_minor > 0),
    CONSTRAINT ck_ledger_entry_currency_code CHECK (currency_code = upper(currency_code)),
    CONSTRAINT ck_ledger_entry_balance_after_non_negative CHECK (balance_after_minor >= 0)
);

CREATE INDEX idx_ledger_entries_wallet_created_at
    ON elitebet.ledger_entries (wallet_id, created_at DESC);

CREATE INDEX idx_ledger_entries_transaction_id
    ON elitebet.ledger_entries (transaction_id);

CREATE INDEX idx_ledger_entries_reference
    ON elitebet.ledger_entries (reference_type, reference_id)
    WHERE reference_type IS NOT NULL AND reference_id IS NOT NULL;

CREATE OR REPLACE FUNCTION elitebet.prevent_ledger_entry_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'ledger_entries is append-only';
END;
$$;

CREATE TRIGGER trg_ledger_entries_append_only_update
    BEFORE UPDATE ON elitebet.ledger_entries
    FOR EACH ROW
    EXECUTE FUNCTION elitebet.prevent_ledger_entry_mutation();

CREATE TRIGGER trg_ledger_entries_append_only_delete
    BEFORE DELETE ON elitebet.ledger_entries
    FOR EACH ROW
    EXECUTE FUNCTION elitebet.prevent_ledger_entry_mutation();
