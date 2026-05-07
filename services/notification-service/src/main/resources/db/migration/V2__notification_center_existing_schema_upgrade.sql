CREATE SCHEMA IF NOT EXISTS notification_schema;

ALTER TABLE IF EXISTS notification_schema.notifications
    ADD COLUMN IF NOT EXISTS metadata JSONB,
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;
