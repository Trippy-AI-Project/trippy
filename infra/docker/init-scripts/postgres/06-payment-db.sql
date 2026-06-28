-- =============================================================================
-- Payment Service Schema Bootstrap
-- =============================================================================

CREATE TABLE IF NOT EXISTS payment_schema.payment_methods (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    last4 VARCHAR(4) NOT NULL,
    brand VARCHAR(50),
    expiry_month INTEGER,
    expiry_year INTEGER,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_methods_user_default
    ON payment_schema.payment_methods (user_id, is_default);

CREATE INDEX IF NOT EXISTS idx_payment_methods_user_brand
    ON payment_schema.payment_methods (user_id, brand);

CREATE TABLE IF NOT EXISTS payment_schema.subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    plan VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_period_start DATE NOT NULL,
    current_period_end DATE NOT NULL,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    price_amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id
    ON payment_schema.subscriptions (user_id);

CREATE TABLE IF NOT EXISTS payment_schema.transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    plan_id VARCHAR(30) NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    type VARCHAR(30) DEFAULT 'SUBSCRIPTION',
    description VARCHAR(512),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_created_at
    ON payment_schema.transactions (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS payment_schema.webhook_events (
    id UUID PRIMARY KEY,
    checkout_session_id TEXT NOT NULL UNIQUE,
    event_type TEXT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_webhook_checkout_session
    ON payment_schema.webhook_events (checkout_session_id);