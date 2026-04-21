CREATE TABLE elitebet.auth_accounts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    principal_id varchar(160) NOT NULL,
    username varchar(160) NOT NULL,
    email varchar(320) NOT NULL,
    status varchar(40) NOT NULL,
    mfa_status varchar(40) NOT NULL,
    provider varchar(40) NOT NULL,
    provider_user_id varchar(160) NOT NULL,
    email_verified boolean NOT NULL DEFAULT false,
    failed_login_attempts integer NOT NULL DEFAULT 0,
    locked_until timestamptz,
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_auth_accounts_principal_id UNIQUE (principal_id),
    CONSTRAINT uq_auth_accounts_email UNIQUE (email),
    CONSTRAINT uq_auth_accounts_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT ck_auth_account_status CHECK (status IN ('PENDING_ACTIVATION', 'ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT ck_auth_account_mfa_status CHECK (mfa_status IN ('NOT_CONFIGURED', 'ENABLED', 'REQUIRED')),
    CONSTRAINT ck_auth_account_provider CHECK (provider IN ('KEYCLOAK')),
    CONSTRAINT ck_auth_failed_login_attempts CHECK (failed_login_attempts >= 0)
);

CREATE INDEX idx_auth_accounts_status
    ON elitebet.auth_accounts (status);

CREATE INDEX idx_auth_accounts_locked_until
    ON elitebet.auth_accounts (locked_until)
    WHERE locked_until IS NOT NULL;
