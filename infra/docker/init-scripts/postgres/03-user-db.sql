-- =============================================================================
-- User Service Schema Bootstrap
-- =============================================================================

CREATE TABLE IF NOT EXISTS user_schema.users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    bio VARCHAR(500),
    phone_number VARCHAR(20),
    country VARCHAR(100),
    avatar_url VARCHAR(2048),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_schema.refresh_tokens (
    id UUID PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    remember_me BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES user_schema.users (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_token
    ON user_schema.refresh_tokens (token);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON user_schema.refresh_tokens (user_id);

CREATE TABLE IF NOT EXISTS user_schema.email_verification_tokens (
    id UUID PRIMARY KEY,
    token TEXT NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES user_schema.users (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_email_verification_tokens_token
    ON user_schema.email_verification_tokens (token);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_id
    ON user_schema.email_verification_tokens (user_id);