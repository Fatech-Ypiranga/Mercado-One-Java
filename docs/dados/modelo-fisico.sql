-- Modelo fisico do Mercado One (PostgreSQL 18), gerado por pg_dump --schema-only apos aplicar as migrations Flyway V1-V8.

CREATE TABLE public.app_users (
    id bigint NOT NULL,
    name character varying(120) NOT NULL,
    login character varying(120) NOT NULL,
    password_hash character varying(255) NOT NULL,
    role character varying(40) NOT NULL,
    active boolean NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
CREATE SEQUENCE public.app_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.app_users_id_seq OWNED BY public.app_users.id;
CREATE TABLE public.audit_events (
    id bigint NOT NULL,
    action character varying(80) NOT NULL,
    entity_type character varying(80) NOT NULL,
    entity_id character varying(80),
    actor_user_id bigint,
    summary_json text NOT NULL,
    created_at timestamp with time zone NOT NULL
);
CREATE SEQUENCE public.audit_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.audit_events_id_seq OWNED BY public.audit_events.id;
CREATE TABLE public.catalog_categories (
    id bigint NOT NULL,
    name character varying(120) NOT NULL,
    active boolean NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
CREATE SEQUENCE public.catalog_categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.catalog_categories_id_seq OWNED BY public.catalog_categories.id;
CREATE TABLE public.catalog_products (
    id bigint NOT NULL,
    name character varying(160) NOT NULL,
    barcode character varying(80),
    sku character varying(80),
    category_id bigint NOT NULL,
    unit character varying(24) NOT NULL,
    sale_price numeric(12,2) NOT NULL,
    active boolean NOT NULL,
    ncm character varying(16),
    cest character varying(16),
    default_cfop character varying(16),
    merchandise_origin character varying(80),
    tax_classification character varying(120),
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
CREATE SEQUENCE public.catalog_products_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.catalog_products_id_seq OWNED BY public.catalog_products.id;
CREATE TABLE public.customers (
    id bigint NOT NULL,
    name character varying(160) NOT NULL,
    phone character varying(40),
    email character varying(160),
    document character varying(40),
    contact_consent boolean NOT NULL,
    active boolean NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
CREATE SEQUENCE public.customers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.customers_id_seq OWNED BY public.customers.id;
CREATE TABLE public.inventory_balances (
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    quantity numeric(14,3) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
CREATE SEQUENCE public.inventory_balances_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.inventory_balances_id_seq OWNED BY public.inventory_balances.id;
CREATE TABLE public.inventory_movements (
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    type character varying(32) NOT NULL,
    quantity_delta numeric(14,3) NOT NULL,
    quantity_before numeric(14,3) NOT NULL,
    quantity_after numeric(14,3) NOT NULL,
    reason character varying(160) NOT NULL,
    supplier_name character varying(160),
    document_number character varying(80),
    note character varying(500),
    created_by_user_id bigint,
    created_at timestamp with time zone NOT NULL,
    supplier_id bigint
);
CREATE SEQUENCE public.inventory_movements_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.inventory_movements_id_seq OWNED BY public.inventory_movements.id;
CREATE TABLE public.offline_sale_conflicts (
    id bigint NOT NULL,
    local_sale_id character varying(80) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    customer_id bigint,
    operator_user_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    conflict_summary text NOT NULL,
    sale_payload text NOT NULL,
    remote_sale_id bigint,
    resolution_note character varying(500),
    resolved_by_user_id bigint,
    resolved_at timestamp with time zone,
    updated_at timestamp with time zone NOT NULL
);
CREATE SEQUENCE public.offline_sale_conflicts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.offline_sale_conflicts_id_seq OWNED BY public.offline_sale_conflicts.id;
CREATE TABLE public.sale_items (
    id bigint NOT NULL,
    sale_id bigint NOT NULL,
    product_id bigint NOT NULL,
    quantity numeric(14,3) NOT NULL,
    unit_price numeric(12,2) NOT NULL,
    total_amount numeric(14,2) NOT NULL
);
CREATE SEQUENCE public.sale_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.sale_items_id_seq OWNED BY public.sale_items.id;
CREATE TABLE public.sale_payments (
    id bigint NOT NULL,
    sale_id bigint NOT NULL,
    method character varying(32) NOT NULL,
    amount numeric(14,2) NOT NULL
);
CREATE SEQUENCE public.sale_payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.sale_payments_id_seq OWNED BY public.sale_payments.id;
CREATE TABLE public.sales (
    id bigint NOT NULL,
    operator_user_id bigint NOT NULL,
    status character varying(32) NOT NULL,
    total_amount numeric(14,2) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    customer_id bigint,
    gross_amount numeric(14,2) NOT NULL,
    discount_amount numeric(14,2) NOT NULL,
    client_sale_id character varying(80),
    canceled_at timestamp with time zone,
    canceled_by_user_id bigint,
    cancel_reason character varying(500)
);
CREATE SEQUENCE public.sales_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.sales_id_seq OWNED BY public.sales.id;
CREATE TABLE public.suppliers (
    id bigint NOT NULL,
    name character varying(160) NOT NULL,
    document character varying(40),
    phone character varying(40),
    email character varying(160),
    notes character varying(500),
    active boolean NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);
CREATE SEQUENCE public.suppliers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER SEQUENCE public.suppliers_id_seq OWNED BY public.suppliers.id;
ALTER TABLE ONLY public.app_users ALTER COLUMN id SET DEFAULT nextval('public.app_users_id_seq'::regclass);
ALTER TABLE ONLY public.audit_events ALTER COLUMN id SET DEFAULT nextval('public.audit_events_id_seq'::regclass);
ALTER TABLE ONLY public.catalog_categories ALTER COLUMN id SET DEFAULT nextval('public.catalog_categories_id_seq'::regclass);
ALTER TABLE ONLY public.catalog_products ALTER COLUMN id SET DEFAULT nextval('public.catalog_products_id_seq'::regclass);
ALTER TABLE ONLY public.customers ALTER COLUMN id SET DEFAULT nextval('public.customers_id_seq'::regclass);
ALTER TABLE ONLY public.inventory_balances ALTER COLUMN id SET DEFAULT nextval('public.inventory_balances_id_seq'::regclass);
ALTER TABLE ONLY public.inventory_movements ALTER COLUMN id SET DEFAULT nextval('public.inventory_movements_id_seq'::regclass);
ALTER TABLE ONLY public.offline_sale_conflicts ALTER COLUMN id SET DEFAULT nextval('public.offline_sale_conflicts_id_seq'::regclass);
ALTER TABLE ONLY public.sale_items ALTER COLUMN id SET DEFAULT nextval('public.sale_items_id_seq'::regclass);
ALTER TABLE ONLY public.sale_payments ALTER COLUMN id SET DEFAULT nextval('public.sale_payments_id_seq'::regclass);
ALTER TABLE ONLY public.sales ALTER COLUMN id SET DEFAULT nextval('public.sales_id_seq'::regclass);
ALTER TABLE ONLY public.suppliers ALTER COLUMN id SET DEFAULT nextval('public.suppliers_id_seq'::regclass);
ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_login_key UNIQUE (login);
ALTER TABLE ONLY public.app_users
    ADD CONSTRAINT app_users_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.audit_events
    ADD CONSTRAINT audit_events_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.catalog_categories
    ADD CONSTRAINT catalog_categories_name_key UNIQUE (name);
ALTER TABLE ONLY public.catalog_categories
    ADD CONSTRAINT catalog_categories_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.catalog_products
    ADD CONSTRAINT catalog_products_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.inventory_balances
    ADD CONSTRAINT inventory_balances_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.inventory_balances
    ADD CONSTRAINT inventory_balances_product_id_key UNIQUE (product_id);
ALTER TABLE ONLY public.inventory_movements
    ADD CONSTRAINT inventory_movements_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.offline_sale_conflicts
    ADD CONSTRAINT offline_sale_conflicts_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.sale_items
    ADD CONSTRAINT sale_items_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.sale_payments
    ADD CONSTRAINT sale_payments_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.sales
    ADD CONSTRAINT sales_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.suppliers
    ADD CONSTRAINT suppliers_pkey PRIMARY KEY (id);
CREATE INDEX ix_audit_events_action ON public.audit_events USING btree (action);
CREATE INDEX ix_audit_events_actor_user_id ON public.audit_events USING btree (actor_user_id);
CREATE INDEX ix_audit_events_created_at ON public.audit_events USING btree (created_at);
CREATE INDEX ix_audit_events_entity ON public.audit_events USING btree (entity_type, entity_id);
CREATE INDEX ix_catalog_products_category_id ON public.catalog_products USING btree (category_id);
CREATE INDEX ix_catalog_products_name ON public.catalog_products USING btree (name);
CREATE INDEX ix_customers_active ON public.customers USING btree (active);
CREATE INDEX ix_customers_document ON public.customers USING btree (document);
CREATE INDEX ix_customers_email ON public.customers USING btree (email);
CREATE INDEX ix_customers_name ON public.customers USING btree (name);
CREATE INDEX ix_customers_phone ON public.customers USING btree (phone);
CREATE INDEX ix_inventory_balances_product_id ON public.inventory_balances USING btree (product_id);
CREATE INDEX ix_inventory_movements_created_at ON public.inventory_movements USING btree (created_at);
CREATE INDEX ix_inventory_movements_product_id ON public.inventory_movements USING btree (product_id);
CREATE INDEX ix_inventory_movements_supplier_id ON public.inventory_movements USING btree (supplier_id);
CREATE INDEX ix_inventory_movements_type ON public.inventory_movements USING btree (type);
CREATE INDEX ix_offline_sale_conflicts_local_sale_id ON public.offline_sale_conflicts USING btree (local_sale_id);
CREATE INDEX ix_offline_sale_conflicts_status ON public.offline_sale_conflicts USING btree (status);
CREATE INDEX ix_offline_sale_conflicts_updated_at ON public.offline_sale_conflicts USING btree (updated_at);
CREATE INDEX ix_sale_items_product_id ON public.sale_items USING btree (product_id);
CREATE INDEX ix_sale_items_sale_id ON public.sale_items USING btree (sale_id);
CREATE INDEX ix_sale_payments_sale_id ON public.sale_payments USING btree (sale_id);
CREATE INDEX ix_sales_created_at ON public.sales USING btree (created_at);
CREATE INDEX ix_sales_customer_id ON public.sales USING btree (customer_id);
CREATE INDEX ix_sales_operator_user_id ON public.sales USING btree (operator_user_id);
CREATE INDEX ix_sales_status ON public.sales USING btree (status);
CREATE INDEX ix_suppliers_active ON public.suppliers USING btree (active);
CREATE INDEX ix_suppliers_document ON public.suppliers USING btree (document);
CREATE INDEX ix_suppliers_name ON public.suppliers USING btree (name);
CREATE UNIQUE INDEX ux_catalog_products_barcode ON public.catalog_products USING btree (barcode) WHERE (barcode IS NOT NULL);
CREATE UNIQUE INDEX ux_catalog_products_sku ON public.catalog_products USING btree (sku) WHERE (sku IS NOT NULL);
CREATE UNIQUE INDEX ux_sales_client_sale_id ON public.sales USING btree (client_sale_id);
ALTER TABLE ONLY public.audit_events
    ADD CONSTRAINT audit_events_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.app_users(id);
ALTER TABLE ONLY public.catalog_products
    ADD CONSTRAINT catalog_products_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.catalog_categories(id);
ALTER TABLE ONLY public.inventory_balances
    ADD CONSTRAINT inventory_balances_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.catalog_products(id);
ALTER TABLE ONLY public.inventory_movements
    ADD CONSTRAINT inventory_movements_created_by_user_id_fkey FOREIGN KEY (created_by_user_id) REFERENCES public.app_users(id);
ALTER TABLE ONLY public.inventory_movements
    ADD CONSTRAINT inventory_movements_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.catalog_products(id);
ALTER TABLE ONLY public.inventory_movements
    ADD CONSTRAINT inventory_movements_supplier_id_fkey FOREIGN KEY (supplier_id) REFERENCES public.suppliers(id);
ALTER TABLE ONLY public.offline_sale_conflicts
    ADD CONSTRAINT offline_sale_conflicts_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);
ALTER TABLE ONLY public.offline_sale_conflicts
    ADD CONSTRAINT offline_sale_conflicts_operator_user_id_fkey FOREIGN KEY (operator_user_id) REFERENCES public.app_users(id);
ALTER TABLE ONLY public.offline_sale_conflicts
    ADD CONSTRAINT offline_sale_conflicts_remote_sale_id_fkey FOREIGN KEY (remote_sale_id) REFERENCES public.sales(id);
ALTER TABLE ONLY public.offline_sale_conflicts
    ADD CONSTRAINT offline_sale_conflicts_resolved_by_user_id_fkey FOREIGN KEY (resolved_by_user_id) REFERENCES public.app_users(id);
ALTER TABLE ONLY public.sale_items
    ADD CONSTRAINT sale_items_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.catalog_products(id);
ALTER TABLE ONLY public.sale_items
    ADD CONSTRAINT sale_items_sale_id_fkey FOREIGN KEY (sale_id) REFERENCES public.sales(id);
ALTER TABLE ONLY public.sale_payments
    ADD CONSTRAINT sale_payments_sale_id_fkey FOREIGN KEY (sale_id) REFERENCES public.sales(id);
ALTER TABLE ONLY public.sales
    ADD CONSTRAINT sales_canceled_by_user_id_fkey FOREIGN KEY (canceled_by_user_id) REFERENCES public.app_users(id);
ALTER TABLE ONLY public.sales
    ADD CONSTRAINT sales_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);
ALTER TABLE ONLY public.sales
    ADD CONSTRAINT sales_operator_user_id_fkey FOREIGN KEY (operator_user_id) REFERENCES public.app_users(id);
