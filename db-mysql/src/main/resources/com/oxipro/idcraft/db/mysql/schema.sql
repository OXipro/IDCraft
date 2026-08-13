CREATE TABLE IF NOT EXISTS accounts (
    uuid            BINARY(16) PRIMARY KEY,
    username        VARCHAR(16) NOT NULL,
    username_lower  VARCHAR(16) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    is_premium      BOOLEAN NOT NULL DEFAULT FALSE,
    registered_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at   DATETIME,
    last_ip         VARCHAR(45),
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until    DATETIME,
    UNIQUE KEY idx_accounts_username_lower (username_lower)
);

CREATE TABLE IF NOT EXISTS account_factors (
    player_uuid BINARY(16) NOT NULL,
    factor      VARCHAR(32) NOT NULL,
    value       TEXT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (player_uuid, factor),
    KEY idx_account_factors_factor_value (factor, value(191))
);
