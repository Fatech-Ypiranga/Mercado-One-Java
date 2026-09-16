create table customers (
    id bigserial primary key,
    name varchar(160) not null,
    phone varchar(40),
    email varchar(160),
    document varchar(40),
    contact_consent boolean not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

alter table sales add column customer_id bigint references customers(id);

create index ix_customers_name on customers(name);
create index ix_customers_phone on customers(phone);
create index ix_customers_email on customers(email);
create index ix_customers_document on customers(document);
create index ix_customers_active on customers(active);
create index ix_sales_customer_id on sales(customer_id);
