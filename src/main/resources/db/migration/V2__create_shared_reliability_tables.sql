CREATE TABLE elitebet.idempotency_records (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_type varchar(80) NOT NULL,
    idempotency_key varchar(160) NOT NULL,
    actor_id varchar(160) NOT NULL,
    request_hash varchar(128) NOT NULL,
    status varchar(40) NOT NULL,
    response_status integer,
    response_body jsonb,
    locked_until timestamptz,
    completed_at timestamptz,
    failure_reason text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_idempotency_operation_type CHECK (operation_type IN ('USER_REGISTRATION', 'BET_PLACEMENT', 'DEPOSIT_CALLBACK', 'WITHDRAWAL_CALLBACK', 'SETTLEMENT_REPLAY', 'WALLET_ADJUSTMENT')),
    CONSTRAINT ck_idempotency_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT ck_idempotency_response_status CHECK (response_status IS NULL OR response_status BETWEEN 100 AND 599),
    CONSTRAINT uq_idempotency_operation_key_actor UNIQUE (operation_type, idempotency_key, actor_id)
);

CREATE INDEX idx_idempotency_records_status_locked_until
    ON elitebet.idempotency_records (status, locked_until);

CREATE TABLE elitebet.outbox_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type varchar(80) NOT NULL,
    aggregate_type varchar(120) NOT NULL,
    aggregate_id varchar(160) NOT NULL,
    event_name varchar(160) NOT NULL,
    payload jsonb NOT NULL,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    status varchar(40) NOT NULL DEFAULT 'PENDING',
    retry_count integer NOT NULL DEFAULT 0,
    next_retry_at timestamptz,
    last_error text,
    occurred_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    published_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_outbox_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX idx_outbox_events_ready
    ON elitebet.outbox_events (status, next_retry_at, created_at)
    WHERE status IN ('PENDING', 'FAILED');

CREATE INDEX idx_outbox_events_aggregate
    ON elitebet.outbox_events (aggregate_type, aggregate_id, occurred_at);

CREATE TABLE elitebet.audit_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id varchar(160) NOT NULL,
    actor_type varchar(40) NOT NULL,
    action varchar(80) NOT NULL,
    category varchar(80) NOT NULL,
    entity_type varchar(120) NOT NULL,
    entity_id varchar(160),
    correlation_id varchar(160),
    ip_address varchar(80),
    user_agent text,
    before_state jsonb,
    after_state jsonb,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_audit_actor_type CHECK (actor_type IN ('USER', 'ADMIN', 'SYSTEM', 'PROVIDER')),
    CONSTRAINT ck_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'APPROVE', 'REJECT', 'ADJUST', 'LOGIN', 'LOGOUT', 'LOCK', 'UNLOCK')),
    CONSTRAINT ck_audit_category CHECK (category IN ('SECURITY', 'COMPLIANCE', 'MONEY', 'BETTING', 'ADMIN', 'SYSTEM'))
);

CREATE INDEX idx_audit_events_actor_occurred_at
    ON elitebet.audit_events (actor_id, occurred_at DESC);

CREATE INDEX idx_audit_events_entity_occurred_at
    ON elitebet.audit_events (entity_type, entity_id, occurred_at DESC);

CREATE INDEX idx_audit_events_correlation_id
    ON elitebet.audit_events (correlation_id)
    WHERE correlation_id IS NOT NULL;

CREATE OR REPLACE FUNCTION elitebet.prevent_audit_event_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'audit_events is append-only';
END;
$$;

CREATE TRIGGER trg_audit_events_append_only_update
    BEFORE UPDATE ON elitebet.audit_events
    FOR EACH ROW
    EXECUTE FUNCTION elitebet.prevent_audit_event_mutation();

CREATE TRIGGER trg_audit_events_append_only_delete
    BEFORE DELETE ON elitebet.audit_events
    FOR EACH ROW
    EXECUTE FUNCTION elitebet.prevent_audit_event_mutation();
