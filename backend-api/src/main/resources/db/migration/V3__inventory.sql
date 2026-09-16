create table inventory_balances (
    id bigserial primary key,
    product_id bigint not null unique references catalog_products(id),
    quantity numeric(14, 3) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table inventory_movements (
    id bigserial primary key,
    product_id bigint not null references catalog_products(id),
    type varchar(32) not null,
    quantity_delta numeric(14, 3) not null,
    quantity_before numeric(14, 3) not null,
    quantity_after numeric(14, 3) not null,
    reason varchar(160) not null,
    supplier_name varchar(160),
    document_number varchar(80),
    note varchar(500),
    created_by_user_id bigint references app_users(id),
    created_at timestamp with time zone not null
);

create index ix_inventory_balances_product_id on inventory_balances(product_id);
create index ix_inventory_movements_product_id on inventory_movements(product_id);
create index ix_inventory_movements_created_at on inventory_movements(created_at);
create index ix_inventory_movements_type on inventory_movements(type);
