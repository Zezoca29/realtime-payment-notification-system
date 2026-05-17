-- Init script for PostgreSQL
-- Runs automatically on first container start

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tables are auto-created by Spring JPA (ddl-auto: update)
-- This file is kept for any DB-level configurations

-- Set timezone
SET timezone = 'UTC';
