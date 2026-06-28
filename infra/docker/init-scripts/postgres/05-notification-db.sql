-- =============================================================================
-- Notification Service Schema Bootstrap
-- =============================================================================

CREATE TABLE IF NOT EXISTS notification_schema.notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    channel VARCHAR(10) NOT NULL,
    action_url VARCHAR(500),
    metadata JSONB,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id
    ON notification_schema.notifications (user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created_at
    ON notification_schema.notifications (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS notification_schema.email_logs (
    id UUID PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    template_name VARCHAR(100),
    status VARCHAR(10) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_message VARCHAR(2000)
);