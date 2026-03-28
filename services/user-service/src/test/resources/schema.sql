-- Creates the user_schema before Hibernate DDL runs in tests.
-- The quoted identifier preserves lowercase to match @Table(schema = "user_schema").
CREATE SCHEMA IF NOT EXISTS "user_schema";
