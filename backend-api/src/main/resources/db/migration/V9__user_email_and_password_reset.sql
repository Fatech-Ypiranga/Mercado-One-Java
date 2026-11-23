alter table app_users add column email varchar(180);

-- E-mail e opcional. Quando existe, nao se repete (sem diferenciar maiusculas de minusculas).
create unique index ux_app_users_email on app_users (lower(email)) where email is not null;

-- Redefinicao de senha por e-mail. Guarda so o hash SHA-256 do token: o token em si so existe no link enviado.
create table password_reset_tokens (
    id bigserial primary key,
    user_id bigint not null references app_users(id),
    token_hash varchar(64) not null unique,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone,
    created_at timestamp with time zone not null
);

create index ix_password_reset_tokens_user_created on password_reset_tokens (user_id, created_at);
