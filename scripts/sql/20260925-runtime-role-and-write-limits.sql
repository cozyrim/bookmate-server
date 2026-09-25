-- Run once as the BookMate database owner, before deploying the API.
-- Set the login password separately through a secure channel; never commit it.
BEGIN;
CREATE TABLE IF NOT EXISTS public.user_write_limits (
    id varchar(80) PRIMARY KEY,
    window_start bigint NOT NULL,
    used integer NOT NULL CHECK (used >= 0)
);
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'bookmate_runtime') THEN
        CREATE ROLE bookmate_runtime NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
    END IF;
END $$;
GRANT USAGE ON SCHEMA public TO bookmate_runtime;
DO $$
DECLARE table_name text;
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO bookmate_runtime', current_database());
    FOREACH table_name IN ARRAY ARRAY[
        'app_users', 'books', 'words', 'quotes', 'reading_memos', 'reviews',
        'guestbook_messages', 'notification_inbox_items', 'notification_device_tokens',
        'moderation_reports', 'user_blocks', 'user_write_limits'
    ] LOOP
        EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('REVOKE ALL ON TABLE public.%I FROM PUBLIC, anon, authenticated', table_name);
        EXECUTE format('GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.%I TO bookmate_runtime', table_name);
        IF NOT EXISTS (SELECT FROM pg_policies WHERE schemaname = 'public'
                       AND tablename = table_name AND policyname = 'bookmate_server_access') THEN
            EXECUTE format('CREATE POLICY bookmate_server_access ON public.%I TO bookmate_runtime USING (true) WITH CHECK (true)', table_name);
        END IF;
    END LOOP;
END $$;
COMMIT;
-- The server enforces individual users' ownership. This role may access BookMate
-- rows, but cannot own/drop tables, create roles, or bypass RLS. New tables need
-- an explicit grant and policy; ddl-auto must stay validate in production.
