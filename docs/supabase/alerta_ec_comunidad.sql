-- ALERTA EC · Comunidad (proyecto Supabase bnnpdwrduktchmgbwlha)
-- Pegar completo en: Supabase > SQL Editor > New query > Run. Se puede ejecutar más de una vez.
-- La app usa SOLO la clave pública (anon / publishable). Nunca pongas la service_role en la app.

-- 1) Reportes meteorológicos comunitarios -----------------------------------
create table if not exists public.community_reports (
  id          bigint generated always as identity primary key,
  created_at  timestamptz not null default now(),
  category    text not null check (char_length(category) between 2 and 60),
  description text not null check (char_length(description) between 5 and 1000),
  -- Coordenadas opcionales, redondeadas a 0,01° (~1 km) por la app; solo Ecuador continental e insular.
  latitude    numeric(5,2) check (latitude  between -6 and 2),
  longitude   numeric(6,2) check (longitude between -92.5 and -75),
  app_version text check (char_length(app_version) <= 40),
  hidden      boolean not null default false   -- moderación desde el panel
);

create index if not exists community_reports_created_idx on public.community_reports (created_at desc);
create index if not exists community_reports_area_idx on public.community_reports (latitude, longitude);

alter table public.community_reports enable row level security;

-- Cualquiera (anon) puede publicar un reporte nuevo, sin elegir id, fecha ni estado de moderación.
drop policy if exists "anon inserta reportes" on public.community_reports;
create policy "anon inserta reportes" on public.community_reports
  for insert to anon, authenticated
  with check (hidden = false and created_at >= now() - interval '1 minute');

-- Cualquiera puede leer reportes visibles de los últimos 3 días (no se exponen más datos).
drop policy if exists "anon lee reportes recientes" on public.community_reports;
create policy "anon lee reportes recientes" on public.community_reports
  for select to anon, authenticated
  using (hidden = false and created_at > now() - interval '3 days');

-- Sin políticas de update/delete: la app no puede modificar ni borrar.
revoke update, delete on public.community_reports from anon, authenticated;

-- 2) Reseñas de la aplicación (solo escritura desde la app) -------------------
create table if not exists public.app_reviews (
  id          bigint generated always as identity primary key,
  created_at  timestamptz not null default now(),
  stars       smallint not null check (stars between 1 and 5),
  comment     text not null check (char_length(comment) between 5 and 1000),
  app_version text check (char_length(app_version) <= 40)
);

alter table public.app_reviews enable row level security;

drop policy if exists "anon inserta resenas" on public.app_reviews;
create policy "anon inserta resenas" on public.app_reviews
  for insert to anon, authenticated
  with check (created_at >= now() - interval '1 minute');

-- No hay política select: las reseñas solo se leen desde el panel de Supabase (service_role).
revoke select, update, delete on public.app_reviews from anon, authenticated;

-- 3) Límite básico anti-abuso: máximo 30 reportes por minuto en total ------------
create or replace function public.alerta_ec_limit_reports() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if (select count(*) from public.community_reports where created_at > now() - interval '1 minute') >= 30 then
    raise exception 'Demasiados reportes en el último minuto' using errcode = 'P0001';
  end if;
  return new;
end $$;

drop trigger if exists alerta_ec_limit_reports on public.community_reports;
create trigger alerta_ec_limit_reports before insert on public.community_reports
  for each row execute function public.alerta_ec_limit_reports();
