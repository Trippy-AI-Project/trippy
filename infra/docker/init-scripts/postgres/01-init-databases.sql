-- =============================================================================
-- Trippy Platform - Database Initialization Script
-- =============================================================================
-- Creates isolated schemas for each microservice.
-- Executed automatically on first PostgreSQL container start.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Create per-service schemas inside the main trippy_db
-- ---------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS user_schema;
CREATE SCHEMA IF NOT EXISTS trip_schema;
CREATE SCHEMA IF NOT EXISTS chat_schema;
CREATE SCHEMA IF NOT EXISTS ai_schema;
CREATE SCHEMA IF NOT EXISTS notification_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;

-- Log successful initialization
DO $$
BEGIN
    RAISE NOTICE 'Trippy database schemas initialized successfully:';
    RAISE NOTICE '  - user_schema';
    RAISE NOTICE '  - trip_schema';
    RAISE NOTICE '  - chat_schema';
    RAISE NOTICE '  - ai_schema';
    RAISE NOTICE '  - notification_schema';
    RAISE NOTICE '  - payment_schema';
END $$;