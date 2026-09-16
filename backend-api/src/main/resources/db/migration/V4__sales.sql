create table sales (
    id bigserial primary key,
    operator_user_id bigint not null references app_users(id),
    status varchar(32) not null,
    total_amount numeric(14, 2) not null,
    created_at timestamp with time zone not null
);

create table sale_items (
    id bigserial primary key,
    sale_id bigint not null references sales(id),
    product_id bigint not null references catalog_products(id),
    quantity numeric(14, 3) not null,
    unit_price numeric(12, 2) not null,
    total_amount numeric(14, 2) not null
);

create table sale_payments (
    id bigserial primary key,
    sale_id bigint not null references sales(id),
    method varchar(32) not null,
    amount numeric(14, 2) not null
);

create index ix_sales_operator_user_id on sales(operator_user_id);
create index ix_sales_created_at on sales(created_at);
create index ix_sale_items_sale_id on sale_items(sale_id);
create index ix_sale_items_product_id on sale_items(product_id);
create index ix_sale_payments_sale_id on sale_payments(sale_id);
