-- Supabase tables accessed exclusively through this server's JDBC connection.
-- Back up the schema and grants first. Verify the server role bypasses RLS.
-- Do not use for clients that access these tables through Supabase Data API.
BEGIN;
SET LOCAL lock_timeout = '2s';
SET LOCAL statement_timeout = '10s';

ALTER TABLE public.app_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.books ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.guestbook_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.moderation_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_device_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_inbox_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quotes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reading_memos ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_blocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.words ENABLE ROW LEVEL SECURITY;

REVOKE ALL PRIVILEGES ON TABLE
    public.app_users, public.books, public.guestbook_messages,
    public.moderation_reports, public.notification_device_tokens,
    public.notification_inbox_items, public.quotes, public.reading_memos,
    public.reviews, public.user_blocks, public.words
FROM anon, authenticated, PUBLIC;

COMMIT;
