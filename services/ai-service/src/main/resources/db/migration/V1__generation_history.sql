CREATE SCHEMA IF NOT EXISTS ai_schema;

CREATE TABLE IF NOT EXISTS ai_schema.generation_history (
    id UUID PRIMARY KEY,
    generation_id UUID NOT NULL UNIQUE,
    trip_id UUID,
    destination VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    prompt_hash VARCHAR(64),
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    request_payload JSONB,
    response_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_generation_history_generation_id
    ON ai_schema.generation_history (generation_id);

CREATE INDEX IF NOT EXISTS idx_generation_history_trip_id
    ON ai_schema.generation_history (trip_id);

CREATE INDEX IF NOT EXISTS idx_generation_history_created_at
    ON ai_schema.generation_history (created_at);

CREATE TABLE IF NOT EXISTS ai_schema.ai_request_logs (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    request_type VARCHAR(30) NOT NULL,
    prompt_hash VARCHAR(64),
    response_time_ms BIGINT,
    input_tokens INTEGER,
    output_tokens INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_request_logs_user_id
    ON ai_schema.ai_request_logs (user_id);
