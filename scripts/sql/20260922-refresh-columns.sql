-- Run with psql -v ON_ERROR_STOP=1, without an outer transaction.
-- Additive change: keep these columns when rolling back the API.
BEGIN;
SET LOCAL lock_timeout = '2s';
SET LOCAL statement_timeout = '10s';
ALTER TABLE public.app_users
    ADD COLUMN IF NOT EXISTS refresh_token_hash varchar(43),
    ADD COLUMN IF NOT EXISTS refresh_token_expires_at timestamp(6) with time zone;
COMMIT;

SET lock_timeout = '2s';
SET statement_timeout = '30s';
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS ux_app_users_refresh_token_hash
    ON public.app_users (refresh_token_hash);
