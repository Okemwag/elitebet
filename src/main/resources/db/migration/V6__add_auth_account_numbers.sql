CREATE SEQUENCE elitebet.account_number_seq
    START WITH 100000000000
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 50;

ALTER TABLE elitebet.auth_accounts
    ADD COLUMN account_number varchar(20);

UPDATE elitebet.auth_accounts
SET account_number = 'EB' || nextval('elitebet.account_number_seq')::text
WHERE account_number IS NULL;

ALTER TABLE elitebet.auth_accounts
    ALTER COLUMN account_number SET NOT NULL;

ALTER TABLE elitebet.auth_accounts
    ADD CONSTRAINT uq_auth_accounts_account_number UNIQUE (account_number),
    ADD CONSTRAINT ck_auth_account_number CHECK (account_number ~ '^EB[0-9]{12}$');
