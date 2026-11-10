# Modelo lógico relacional

Este MLR deriva do schema real do PostgreSQL, obtido aplicando as migrations Flyway V1–V8 e extraindo o resultado com `pg_dump`. O modelo físico completo (tipos, defaults, índices e constraints) está em [assets/modelo-fisico.sql](assets/modelo-fisico.sql). O significado de cada campo está no [dicionário de dados](dicionario-de-dados.md) e os conceitos de negócio, no [MER conceitual](modelo-conceitual.md).

Notação: **sublinhado** = chave primária; `#campo` = chave estrangeira (→ tabela referenciada); `[U]` = valor único; `?` = aceita nulo.

## Relações

- **app_users** (<u>id</u>, name, login [U], password_hash, role, active, created_at, updated_at)
- **catalog_categories** (<u>id</u>, name [U], active, created_at, updated_at)
- **catalog_products** (<u>id</u>, name, barcode? [U], sku? [U], #category_id → catalog_categories, unit, sale_price, active, ncm?, cest?, default_cfop?, merchandise_origin?, tax_classification?, created_at, updated_at)
- **inventory_balances** (<u>id</u>, #product_id [U] → catalog_products, quantity, created_at, updated_at)
- **suppliers** (<u>id</u>, name, document?, phone?, email?, notes?, active, created_at, updated_at)
- **inventory_movements** (<u>id</u>, #product_id → catalog_products, type, quantity_delta, quantity_before, quantity_after, reason, supplier_name?, #supplier_id? → suppliers, document_number?, note?, #created_by_user_id? → app_users, created_at)
- **customers** (<u>id</u>, name, phone?, email?, document?, contact_consent, active, created_at, updated_at)
- **sales** (<u>id</u>, #operator_user_id → app_users, #customer_id? → customers, status, gross_amount, discount_amount, total_amount, client_sale_id? [U], canceled_at?, #canceled_by_user_id? → app_users, cancel_reason?, created_at)
- **sale_items** (<u>id</u>, #sale_id → sales, #product_id → catalog_products, quantity, unit_price, total_amount)
- **sale_payments** (<u>id</u>, #sale_id → sales, method, amount)
- **offline_sale_conflicts** (<u>id</u>, local_sale_id, #customer_id? → customers, #operator_user_id → app_users, status, conflict_summary, sale_payload, #remote_sale_id? → sales, resolution_note?, #resolved_by_user_id? → app_users, resolved_at?, created_at, updated_at)
- **audit_events** (<u>id</u>, action, entity_type, entity_id?, #actor_user_id? → app_users, summary_json, created_at)

## Relacionamentos e cardinalidades

| Origem (FK) | Destino | Cardinalidade | Observação |
| --- | --- | --- | --- |
| catalog_products.category_id | catalog_categories | N:1 obrigatório | Todo produto tem categoria. |
| inventory_balances.product_id | catalog_products | 1:1 opcional | Um saldo por produto, criado na primeira movimentação. |
| inventory_movements.product_id | catalog_products | N:1 obrigatório | Histórico imutável. |
| inventory_movements.supplier_id | suppliers | N:1 opcional | Só em entradas com fornecedor cadastrado. |
| inventory_movements.created_by_user_id | app_users | N:1 opcional | Autor da movimentação. |
| sales.operator_user_id | app_users | N:1 obrigatório | Operador da venda. |
| sales.customer_id | customers | N:1 opcional | Cliente identificado. |
| sales.canceled_by_user_id | app_users | N:1 opcional | Preenchido só em venda cancelada. |
| sale_items.sale_id | sales | N:1 obrigatório | A aplicação exige ao menos um item. |
| sale_items.product_id | catalog_products | N:1 obrigatório | Preço praticado fica no item. |
| sale_payments.sale_id | sales | N:1 obrigatório | A aplicação exige ao menos um pagamento. |
| offline_sale_conflicts.operator_user_id | app_users | N:1 obrigatório | Operador da venda local. |
| offline_sale_conflicts.customer_id | customers | N:1 opcional | |
| offline_sale_conflicts.remote_sale_id | sales | N:1 opcional | Venda criada no aceite. |
| offline_sale_conflicts.resolved_by_user_id | app_users | N:1 opcional | Quem resolveu. |
| audit_events.actor_user_id | app_users | N:1 opcional | O alvo do evento (`entity_type`, `entity_id`) é textual, sem FK. |

## Normalização

As relações estão na 3FN: cada atributo não chave depende só da chave primária. Há três redundâncias controladas, mantidas de propósito:

- `sales.total_amount` = `gross_amount` − `discount_amount`, e `sale_items.total_amount` = `quantity` × `unit_price`. Os valores ficam gravados para preservar o que foi praticado na venda.
- `inventory_balances.quantity` resume o histórico de `inventory_movements`, com `quantity_before` e `quantity_after` em cada movimento.
- `inventory_movements.supplier_name` guarda o fornecedor textual de entradas sem cadastro.

## Banco local do PDV (SQLite)

O PDV mantém um banco separado, sem integridade referencial com o servidor:

- **offline_sales** (<u>local_sale_id</u>, created_at, customer_id?, discount_amount, status, remote_sale_id?, last_error?, sync_attempts, updated_at)
- **offline_sale_items** (<u>id</u>, #local_sale_id → offline_sales, product_id, quantity, unit_price)
- **offline_sale_payments** (<u>id</u>, #local_sale_id → offline_sales, method, amount)
- **offline_sale_sync_attempts** (<u>id</u>, #local_sale_id → offline_sales, status, message?, created_at)
- **local_catalog_products** (<u>product_id</u>, name, barcode?, sku?, unit, sale_price, updated_at)

`customer_id`, `product_id` e `remote_sale_id` guardam IDs do servidor sem chave estrangeira. No servidor, `sales.client_sale_id` recebe o `local_sale_id` e garante que cada venda local gere no máximo uma venda.
