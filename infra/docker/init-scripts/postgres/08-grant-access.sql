-- =============================================================================
-- Trippy Platform - Shared Access Grants
-- =============================================================================
-- Grants schema, table, and sequence access to the default application user.
-- This script should run after schema creation and table bootstrap scripts.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. Grant schema usage to the default application user
-- ---------------------------------------------------------------------------
GRANT USAGE  ON SCHEMA user_schema         TO trippy_admin;
GRANT USAGE  ON SCHEMA trip_schema         TO trippy_admin;
GRANT USAGE  ON SCHEMA chat_schema         TO trippy_admin;
GRANT USAGE  ON SCHEMA ai_schema           TO trippy_admin;
GRANT USAGE  ON SCHEMA notification_schema TO trippy_admin;
GRANT USAGE  ON SCHEMA payment_schema      TO trippy_admin;

-- ---------------------------------------------------------------------------
-- 2. Grant privileges on existing tables and sequences
-- ---------------------------------------------------------------------------
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA user_schema         TO trippy_admin;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA trip_schema         TO trippy_admin;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA chat_schema         TO trippy_admin;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA ai_schema           TO trippy_admin;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA notification_schema TO trippy_admin;
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA payment_schema      TO trippy_admin;

GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA user_schema         TO trippy_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA trip_schema         TO trippy_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA chat_schema         TO trippy_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA ai_schema           TO trippy_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA notification_schema TO trippy_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA payment_schema      TO trippy_admin;

-- ---------------------------------------------------------------------------
-- 3. Ensure future tables and sequences inherit the same grants
-- ---------------------------------------------------------------------------
ALTER DEFAULT PRIVILEGES IN SCHEMA user_schema         GRANT ALL ON TABLES    TO trippy_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA trip_schema         GRANT ALL ON TABLES    TO trippy_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA chat_schema         GRANT ALL ON TABLES    TO trippy_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA ai_schema           GRANT ALL ON TABLES    TO trippy_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA notification_schema GRANT ALL ON TABLES    TO trippy_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA payment_schema      GRANT ALL ON TABLES    TO trippy_admin;

ALTER DEFAULT PRIVILEGES IN SCHEMA user_schema         GRANT ALL ON SEQUENCES TO trippy_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA trip_schema         GRANT ALL ON SEQUENCES TO trippy_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA chat_schema         GRANT ALL ON SEQUENCES TO trippy_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA ai_schema           GRANT ALL ON SEQUENCES TO trippy_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA notification_schema GRANT ALL ON SEQUENCES TO trippy_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA payment_schema      GRANT ALL ON SEQUENCES TO trippy_admin;