-- SKYRAS TV user_preferences
-- Run this in the Supabase SQL editor (or `supabase db push`) after creating a project.
-- This is NOT a production auth product: identity is the username column.

create table if not exists public.user_preferences (
    username text primary key,
    m3u_url text,
    prefs jsonb not null default '{}'::jsonb,
    updated_at timestamptz not null default now()
);

comment on table public.user_preferences is 'Per-user IPTV playlist URL and JSON prefs (favorites, last channel).';

alter table public.user_preferences enable row level security;

-- DEV / sideload policies: the Android app uses the anon key and username as the row key.
-- Tighten these before any public rollout (replace with auth.uid() once real auth exists).
drop policy if exists user_preferences_anon_select on public.user_preferences;
drop policy if exists user_preferences_anon_insert on public.user_preferences;
drop policy if exists user_preferences_anon_update on public.user_preferences;

create policy user_preferences_anon_select
    on public.user_preferences for select
    to anon
    using (true);

create policy user_preferences_anon_insert
    on public.user_preferences for insert
    to anon
    with check (true);

create policy user_preferences_anon_update
    on public.user_preferences for update
    to anon
    using (true)
    with check (true);

grant select, insert, update on public.user_preferences to anon, authenticated;
