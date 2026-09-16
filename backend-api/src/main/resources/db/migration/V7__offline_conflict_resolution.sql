create table offline_sale_conflicts (
    id bigserial primary key,
    local_sale_id varchar(80) not null,
    created_at timestamp with time zone not null,
    customer_id bigint references customers(id),
    operator_user_id bigint not null references app_users(id),
    status varchar(20) not null,
    conflict_summary text not null,
    sale_payload text not null,
    remote_sale_id bigint references sales(id),
    resolution_note varchar(500),
    resolved_by_user_id bigint references app_users(id),
    resolved_at timestamp with time zone,
    updated_at timestamp with time zone not null
);

create index ix_offline_sale_conflicts_status on offline_sale_conflicts(status);
create index ix_offline_sale_conflicts_local_sale_id on offline_sale_conflicts(local_sale_id);
create index ix_offline_sale_conflicts_updated_at on offline_sale_conflicts(updated_at);
