create table user_account (
    id bigint generated always as identity,
    login varchar(128) not null,
    role varchar(16) not null,
    blocked boolean not null default false,
    password_hash text,
    session_version bigint not null default 0,
    password_token_hash varchar(64),
    password_token_expires_at timestamptz,
    constraint user_account_pk primary key (id)
);

create unique index user_account_login_uidx on user_account (login);
create unique index user_account_password_token_hash_uidx on user_account (password_token_hash);
