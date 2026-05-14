-- =============================================================================
-- Trippy Platform - Database Initialization Script
-- =============================================================================
-- Creates isolated schemas for each microservice and the SonarQube database.
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

-- ---------------------------------------------------------------------------
-- 2. Grant usage to the default application user
-- ---------------------------------------------------------------------------
GRANT USAGE  ON SCHEMA user_schema         TO trippy_admin;
GRANT USAGE  ON SCHEMA trip_schema         TO trippy_admin;
GRANT USAGE  ON SCHEMA chat_schema         TO trippy_admin;
GRANT USAGE  ON SCHEMA ai_schema           TO trippy_admin;
GRANT USAGE  ON SCHEMA notification_schema TO trippy_admin;
GRANT USAGE  ON SCHEMA payment_schema      TO trippy_admin;

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

-- Ensure future tables/sequences inherit the grants
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

-- ---------------------------------------------------------------------------
-- 3. Create the SonarQube database and user
-- ---------------------------------------------------------------------------
-- SonarQube requires its own database.  docker-entrypoint-initdb.d scripts
-- run inside the POSTGRES_DB, so we create the extra DB here.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sonar_user') THEN
        CREATE ROLE sonar_user WITH LOGIN PASSWORD 'CHANGE_ME_SONAR_DB_PASSWORD!';
    END IF;
END
$$;

-- The CREATE DATABASE statement cannot run inside a transaction block, so
-- we use a separate connection trick via \gexec:
SELECT 'CREATE DATABASE sonarqube_db OWNER sonar_user'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'sonarqube_db') \gexec
