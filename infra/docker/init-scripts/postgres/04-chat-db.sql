-- =============================================================================
-- Chat Service Schema Bootstrap
-- =============================================================================

CREATE TABLE IF NOT EXISTS chat_schema.chat_rooms (
    id UUID PRIMARY KEY,
    trip_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS chat_schema.chat_messages (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    sender_display_name VARCHAR(100) NOT NULL,
    content VARCHAR(4000),
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited_at TIMESTAMPTZ,
    CONSTRAINT fk_chat_messages_room
        FOREIGN KEY (room_id)
        REFERENCES chat_schema.chat_rooms (id)
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_room_id_created_at
    ON chat_schema.chat_messages (room_id, created_at DESC);

CREATE TABLE IF NOT EXISTS chat_schema.message_attachments (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    file_name TEXT NOT NULL,
    file_url VARCHAR(2048) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    thumbnail_url VARCHAR(2048),
    CONSTRAINT fk_message_attachments_message
        FOREIGN KEY (message_id)
        REFERENCES chat_schema.chat_messages (id)
);

CREATE INDEX IF NOT EXISTS idx_message_attachments_message_id
    ON chat_schema.message_attachments (message_id);