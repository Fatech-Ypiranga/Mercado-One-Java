alter table sales add column gross_amount numeric(14, 2);
alter table sales add column discount_amount numeric(14, 2);

update sales set gross_amount = total_amount, discount_amount = 0;

alter table sales alter column gross_amount set not null;
alter table sales alter column discount_amount set not null;

alter table sales add column client_sale_id varchar(80);
alter table sales add column canceled_at timestamp with time zone;
alter table sales add column canceled_by_user_id bigint references app_users(id);
alter table sales add column cancel_reason varchar(500);

create unique index ux_sales_client_sale_id on sales(client_sale_id);
create index ix_sales_status on sales(status);
create index ix_audit_events_action on audit_events(action);
