CREATE TABLE elitebet.user_profiles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    principal_id varchar(160) NOT NULL,
    first_name varchar(80) NOT NULL,
    last_name varchar(80) NOT NULL,
    date_of_birth date NOT NULL,
    country_code varchar(2) NOT NULL,
    region_code varchar(80),
    phone_number varchar(32) NOT NULL,
    status varchar(40) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_profiles_principal_id UNIQUE (principal_id),
    CONSTRAINT ck_user_profile_status CHECK (status IN ('COMPLETE')),
    CONSTRAINT ck_user_profile_country_code CHECK (country_code = upper(country_code)),
    CONSTRAINT ck_user_profile_date_of_birth CHECK (date_of_birth >= DATE '1900-01-01'),
    CONSTRAINT ck_user_profile_phone_number CHECK (phone_number ~ '^\\+[1-9][0-9]{7,14}$')
);

CREATE INDEX idx_user_profiles_country_region
    ON elitebet.user_profiles (country_code, region_code);
