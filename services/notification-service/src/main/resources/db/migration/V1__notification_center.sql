CREATE SCHEMA IF NOT EXISTS notification_schema;

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
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id
    ON notification_schema.notifications (user_id);

CREATE INDEX IF NOT EXISTS idx_notifications_user_created
    ON notification_schema.notifications (user_id, created_at DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
    ON notification_schema.notifications (user_id, is_read)
    WHERE is_deleted = FALSE;

CREATE TABLE IF NOT EXISTS notification_schema.notification_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_preferences_user_type UNIQUE (user_id, type)
);

CREATE INDEX IF NOT EXISTS idx_notification_preferences_user_id
    ON notification_schema.notification_preferences (user_id);

CREATE TABLE IF NOT EXISTS notification_schema.device_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    device_token TEXT NOT NULL,
    platform VARCHAR(20) NOT NULL,
    device_name VARCHAR(120),
    last_active_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_tokens_token UNIQUE (device_token)
);

CREATE INDEX IF NOT EXISTS idx_device_tokens_user_id
    ON notification_schema.device_tokens (user_id);

CREATE TABLE IF NOT EXISTS notification_schema.email_logs (
    id UUID PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    template_name VARCHAR(100),
    status VARCHAR(10) NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    error_message VARCHAR(2000)
);

CREATE INDEX IF NOT EXISTS idx_email_logs_status
    ON notification_schema.email_logs (status);
