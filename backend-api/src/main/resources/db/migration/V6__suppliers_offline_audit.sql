create table suppliers (
    id bigserial primary key,
    name varchar(160) not null,
    document varchar(40),
    phone varchar(40),
    email varchar(160),
    notes varchar(500),
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

alter table inventory_movements add column supplier_id bigint references suppliers(id);

create table audit_events (
    id bigserial primary key,
    action varchar(80) not null,
    entity_type varchar(80) not null,
    entity_id varchar(80),
    actor_user_id bigint references app_users(id),
    summary_json text not null,
    created_at timestamp with time zone not null
);

create index ix_suppliers_name on suppliers(name);
create index ix_suppliers_document on suppliers(document);
create index ix_suppliers_active on suppliers(active);
create index ix_inventory_movements_supplier_id on inventory_movements(supplier_id);
create index ix_audit_events_entity on audit_events(entity_type, entity_id);
create index ix_audit_events_actor_user_id on audit_events(actor_user_id);
create index ix_audit_events_created_at on audit_events(created_at);
