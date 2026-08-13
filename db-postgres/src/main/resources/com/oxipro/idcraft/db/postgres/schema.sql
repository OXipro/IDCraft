-- IDCraft - Postgres schema. Applied manually for now (no Flyway).

CREATE TABLE IF NOT EXISTS accounts (
    uuid            UUID PRIMARY KEY,
    username        VARCHAR(16) NOT NULL,
    username_lower  VARCHAR(16) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    is_premium      BOOLEAN NOT NULL DEFAULT FALSE,
    registered_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at   TIMESTAMPTZ,
    last_ip         VARCHAR(45),
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_accounts_username_lower ON accounts (username_lower);

CREATE TABLE IF NOT EXISTS account_factors (
    player_uuid UUID NOT NULL,
    factor      VARCHAR(32) NOT NULL,
    value       TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (player_uuid, factor)
);
CREATE INDEX IF NOT EXISTS idx_account_factors_factor_value ON account_factors (factor, value);
