-- =============================================================================
-- Init script for PostgreSQL — runs automatically on first container start
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set timezone
SET timezone = 'UTC';

-- =============================================================================
-- Performance Indexes
-- Applied after Spring JPA creates the tables on first startup.
-- Run this script manually if tables already exist:
--   docker exec -it postgres psql -U payments_user -d payments_db -f /docker-entrypoint-initdb.d/init.sql
-- =============================================================================

-- payments table — supports GET /api/v1/payments?customerId=X and listing by date
CREATE INDEX IF NOT EXISTS idx_payments_customer_id   ON payments (customer_id);
CREATE INDEX IF NOT EXISTS idx_payments_status        ON payments (status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at    ON payments (created_at DESC);
-- Compound: list payments by customer sorted by date (single-scan for the common query)
CREATE INDEX IF NOT EXISTS idx_payments_customer_date ON payments (customer_id, created_at DESC);

-- payment_notifications table — supports consumer queries + idempotency lookups
-- (unique idx_event_id is created by JPA @Index; add the rest here)
CREATE INDEX IF NOT EXISTS idx_notifications_customer_id   ON payment_notifications (customer_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status        ON payment_notifications (status);
CREATE INDEX IF NOT EXISTS idx_notifications_processed_at  ON payment_notifications (processed_at DESC);
-- Compound: paginated consumer query "find all for customer, newest first"
CREATE INDEX IF NOT EXISTS idx_notifications_customer_date ON payment_notifications (customer_id, processed_at DESC);

-- payment_outbox table — outbox relay scans PENDING rows ordered by creation time
-- (idx_outbox_status_created is declared on the JPA entity; listed here for documentation)
-- CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON payment_outbox (status, created_at);

