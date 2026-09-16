create table app_users (
    id bigserial primary key,
    name varchar(120) not null,
    login varchar(120) not null unique,
    password_hash varchar(255) not null,
    role varchar(40) not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table catalog_categories (
    id bigserial primary key,
    name varchar(120) not null unique,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table catalog_products (
    id bigserial primary key,
    name varchar(160) not null,
    barcode varchar(80),
    sku varchar(80),
    category_id bigint not null references catalog_categories(id),
    unit varchar(24) not null,
    sale_price numeric(12, 2) not null,
    active boolean not null,
    ncm varchar(16),
    cest varchar(16),
    default_cfop varchar(16),
    merchandise_origin varchar(80),
    tax_classification varchar(120),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_catalog_products_barcode
    on catalog_products (barcode)
    where barcode is not null;

create unique index ux_catalog_products_sku
    on catalog_products (sku)
    where sku is not null;

create index ix_catalog_products_category_id on catalog_products(category_id);
create index ix_catalog_products_name on catalog_products(name);
