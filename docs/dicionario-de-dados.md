# Dicionário de dados

Este documento registra o esquema físico implementado no PostgreSQL da API e no SQLite do PDV. Para entidades, relacionamentos e cardinalidades de negócio, consulte o [MER conceitual](modelo-conceitual.md).

Fontes: [migrações Flyway V1–V7](../backend-api/src/main/resources/db/migration/), [OfflineSaleQueue.java](../pdv-desktop/src/main/java/com/mercadoone/pdv/offline/OfflineSaleQueue.java) e [OfflineCatalogStore.java](../pdv-desktop/src/main/java/com/mercadoone/pdv/offline/OfflineCatalogStore.java).

## Convenções do dicionário

- **PK**: chave primária; **FK**: chave estrangeira; **UK**: valor único; **N**: coluna `NOT NULL`; **O**: aceita `NULL`.
- Em PostgreSQL, `bigserial` gera identificador; `timestamptz` abaixo abrevia `timestamp with time zone`. Campos `varchar` sem domínio SQL fechado são validados principalmente pela aplicação.
- No SQLite, valores monetários e quantidades são guardados como `text` decimal; instantes também como `text` ISO-8601. `integer` de referência remota não tem FK para o PostgreSQL.

## Dicionário — PostgreSQL

### Acesso e catálogo

| Tabela | Campo | Tipo | Regra | Significado |
| --- | --- | --- | --- | --- |
| `app_users` | `id` | bigserial | PK, N | Identificador do usuário. |
| | `name` | varchar(120) | N | Nome exibido. |
| | `login` | varchar(120) | UK, N | Login de autenticação. |
| | `password_hash` | varchar(255) | N | Hash da senha; dado sensível. |
| | `role` | varchar(40) | N | Perfil de acesso. |
| | `active` | boolean | N | Habilitação do usuário. |
| | `created_at`, `updated_at` | timestamptz | N | Criação e última atualização. |
| `catalog_categories` | `id` | bigserial | PK, N | Identificador da categoria. |
| | `name` | varchar(120) | UK, N | Nome da categoria. |
| | `active` | boolean | N | Categoria ativa. |
| | `created_at`, `updated_at` | timestamptz | N | Criação e última atualização. |
| `catalog_products` | `id` | bigserial | PK, N | Identificador do produto. |
| | `name` | varchar(160) | N | Nome comercial. |
| | `barcode` | varchar(80) | O, UK parcial | Código de barras, único quando informado. |
| | `sku` | varchar(80) | O, UK parcial | Código interno, único quando informado. |
| | `category_id` | bigint | FK → `catalog_categories.id`, N | Categoria do produto. |
| | `unit` | varchar(24) | N | Unidade de comercialização. |
| | `sale_price` | numeric(12,2) | N | Preço de venda vigente. |
| | `active` | boolean | N | Disponibilidade para novas vendas. |
| | `ncm`, `cest`, `default_cfop` | varchar(16) | O | Campos fiscais preparatórios. |
| | `merchandise_origin` | varchar(80) | O | Origem da mercadoria. |
| | `tax_classification` | varchar(120) | O | Classificação tributária. |
| | `created_at`, `updated_at` | timestamptz | N | Criação e última atualização. |

### Estoque e fornecedores

| Tabela | Campo | Tipo | Regra | Significado |
| --- | --- | --- | --- | --- |
| `inventory_balances` | `id` | bigserial | PK, N | Identificador do saldo. |
| | `product_id` | bigint | FK → `catalog_products.id`, UK, N | Um saldo por produto. |
| | `quantity` | numeric(14,3) | N | Quantidade atual. |
| | `created_at`, `updated_at` | timestamptz | N | Criação e atualização do saldo. |
| `inventory_movements` | `id` | bigserial | PK, N | Identificador da movimentação. |
| | `product_id` | bigint | FK → `catalog_products.id`, N | Produto movimentado. |
| | `type` | varchar(32) | N | Entrada, ajuste ou baixa por venda. |
| | `quantity_delta` | numeric(14,3) | N | Variação assinada do saldo. |
| | `quantity_before`, `quantity_after` | numeric(14,3) | N | Saldo anterior e posterior. |
| | `reason` | varchar(160) | N | Motivo registrado. |
| | `supplier_name` | varchar(160) | O | Nome textual do fornecedor na movimentação. |
| | `supplier_id` | bigint | FK → `suppliers.id`, O | Fornecedor cadastrado, quando vinculado. |
| | `document_number` | varchar(80) | O | Referência de documento. |
| | `note` | varchar(500) | O | Observação. |
| | `created_by_user_id` | bigint | FK → `app_users.id`, O | Usuário autor, quando identificado. |
| | `created_at` | timestamptz | N | Momento da movimentação. |
| `suppliers` | `id` | bigserial | PK, N | Identificador do fornecedor. |
| | `name` | varchar(160) | N | Nome do fornecedor. |
| | `document` | varchar(40) | O | Documento informado. |
| | `phone` | varchar(40) | O | Telefone. |
| | `email` | varchar(160) | O | E-mail. |
| | `notes` | varchar(500) | O | Observações. |
| | `active` | boolean | N | Cadastro ativo. |
| | `created_at`, `updated_at` | timestamptz | N | Criação e última atualização. |

### Clientes e vendas

| Tabela | Campo | Tipo | Regra | Significado |
| --- | --- | --- | --- | --- |
| `customers` | `id` | bigserial | PK, N | Identificador do cliente. |
| | `name` | varchar(160) | N | Nome. |
| | `phone` | varchar(40) | O | Telefone. |
| | `email` | varchar(160) | O | E-mail. |
| | `document` | varchar(40) | O | Documento informado. |
| | `contact_consent` | boolean | N | Consentimento para contato. |
| | `active` | boolean | N | Cadastro ativo. |
| | `created_at`, `updated_at` | timestamptz | N | Criação e última atualização. |
| `sales` | `id` | bigserial | PK, N | Identificador da venda confirmada. |
| | `operator_user_id` | bigint | FK → `app_users.id`, N | Operador responsável. |
| | `customer_id` | bigint | FK → `customers.id`, O | Cliente identificado, se houver. |
| | `status` | varchar(32) | N | Estado da venda. |
| | `total_amount` | numeric(14,2) | N | Total da venda. |
| | `created_at` | timestamptz | N | Momento da venda. |
| `sale_items` | `id` | bigserial | PK, N | Identificador do item. |
| | `sale_id` | bigint | FK → `sales.id`, N | Venda que contém o item. |
| | `product_id` | bigint | FK → `catalog_products.id`, N | Produto vendido. |
| | `quantity` | numeric(14,3) | N | Quantidade vendida. |
| | `unit_price` | numeric(12,2) | N | Preço unitário praticado. |
| | `total_amount` | numeric(14,2) | N | Total do item. |
| `sale_payments` | `id` | bigserial | PK, N | Identificador do pagamento. |
| | `sale_id` | bigint | FK → `sales.id`, N | Venda paga. |
| | `method` | varchar(32) | N | Meio de pagamento. |
| | `amount` | numeric(14,2) | N | Valor pago por esse meio. |

### Conflitos e auditoria

| Tabela | Campo | Tipo | Regra | Significado |
| --- | --- | --- | --- | --- |
| `offline_sale_conflicts` | `id` | bigserial | PK, N | Identificador da pendência de sincronização. |
| | `local_sale_id` | varchar(80) | N | ID da venda gerado pelo PDV; sem UK/FK no servidor. |
| | `created_at`, `updated_at` | timestamptz | N | Criação e última atualização da pendência. |
| | `customer_id` | bigint | FK → `customers.id`, O | Cliente informado na venda local. |
| | `operator_user_id` | bigint | FK → `app_users.id`, N | Operador da venda local. |
| | `status` | varchar(20) | N | Estado da resolução administrativa. |
| | `conflict_summary` | text | N | Resumo do motivo do conflito. |
| | `sale_payload` | text | N | Dados originais da venda local preservados como texto JSON. |
| | `remote_sale_id` | bigint | FK → `sales.id`, O | Venda confirmada após aceite, quando houver. |
| | `resolution_note` | varchar(500) | O | Nota da resolução. |
| | `resolved_by_user_id` | bigint | FK → `app_users.id`, O | Usuário que resolveu. |
| | `resolved_at` | timestamptz | O | Momento da resolução. |
| `audit_events` | `id` | bigserial | PK, N | Identificador do evento. |
| | `action` | varchar(80) | N | Ação auditada. |
| | `entity_type` | varchar(80) | N | Tipo textual da entidade alvo. |
| | `entity_id` | varchar(80) | O | ID textual da entidade alvo; sem FK. |
| | `actor_user_id` | bigint | FK → `app_users.id`, O | Usuário autor, se identificado. |
| | `summary_json` | text | N | Resumo estruturado serializado como texto JSON. |
| | `created_at` | timestamptz | N | Momento do evento. |

## Dicionário — SQLite do PDV

| Tabela | Campo | Tipo | Regra | Significado |
| --- | --- | --- | --- | --- |
| `offline_sales` | `local_sale_id` | text | PK, N | ID da venda no PDV. |
| | `created_at` | text | N | Data/hora de criação em ISO-8601. |
| | `customer_id` | integer | O | ID lógico do cliente no servidor. |
| | `status` | text | N | Situação local da sincronização. |
| | `remote_sale_id` | integer | O | ID lógico da venda confirmada no servidor. |
| | `last_error` | text | O | Última mensagem de erro ou conflito. |
| | `sync_attempts` | integer | N, padrão 0 | Número de tentativas registradas. |
| | `updated_at` | text | N | Última atualização em ISO-8601. |
| `offline_sale_items` | `id` | integer | PK, autoincremento | Identificador local do item. |
| | `local_sale_id` | text | FK → `offline_sales.local_sale_id`, N | Venda local que contém o item. |
| | `product_id` | integer | N | ID lógico do produto no servidor. |
| | `quantity`, `unit_price` | text | N | Quantidade e preço decimal praticado, em texto. |
| `offline_sale_payments` | `id` | integer | PK, autoincremento | Identificador local do pagamento. |
| | `local_sale_id` | text | FK → `offline_sales.local_sale_id`, N | Venda local paga. |
| | `method` | text | N | Meio de pagamento. |
| | `amount` | text | N | Valor decimal, em texto. |
| `offline_sale_sync_attempts` | `id` | integer | PK, autoincremento | Identificador da tentativa. |
| | `local_sale_id` | text | FK → `offline_sales.local_sale_id`, N | Venda local sincronizada. |
| | `status` | text | N | Resultado da tentativa. |
| | `message` | text | O | Mensagem recebida ou erro. |
| | `created_at` | text | N | Momento da tentativa em ISO-8601. |
| `local_catalog_products` | `product_id` | integer | PK, N | ID do produto no servidor. |
| | `name` | text | N | Nome copiado do catálogo. |
| | `barcode`, `sku` | text | O | Códigos copiados do catálogo. |
| | `unit` | text | N | Unidade de comercialização. |
| | `sale_price` | text | N | Preço decimal copiado, em texto. |
| | `updated_at` | text | N | Momento da atualização local. |

## Domínios e regras relevantes

| Campo | Valores implementados | Fonte |
| --- | --- | --- |
| `app_users.role` | `ADMIN`, `GERENTE`, `OPERADOR_CAIXA`, `ESTOQUISTA` | [UserRole.java](../backend-api/src/main/java/com/mercadoone/backend/modules/access/domain/UserRole.java) |
| `inventory_movements.type` | `ENTRY`, `ADJUSTMENT`, `SALE` | [InventoryMovementType.java](../backend-api/src/main/java/com/mercadoone/backend/modules/inventory/domain/InventoryMovementType.java) |
| `sales.status` | `CONFIRMED` | [SaleStatus.java](../backend-api/src/main/java/com/mercadoone/backend/modules/sales/domain/SaleStatus.java) |
| `sale_payments.method`, `offline_sale_payments.method` | `CASH`, `CARD`, `PIX`, `STORE_CREDIT` | [PaymentMethod da API](../backend-api/src/main/java/com/mercadoone/backend/modules/sales/domain/PaymentMethod.java) e [PaymentMethod do PDV](../pdv-desktop/src/main/java/com/mercadoone/pdv/sales/PaymentMethod.java) |
| `offline_sale_conflicts.status` | `PENDING`, `ACCEPTED`, `REJECTED` | [OfflineConflictStatus.java](../backend-api/src/main/java/com/mercadoone/backend/modules/offline/domain/OfflineConflictStatus.java) |
| `offline_sales.status` | `PENDING`, `SENT`, `CONFLICT`, `ERROR` | [LocalSaleStatus.java](../pdv-desktop/src/main/java/com/mercadoone/pdv/offline/LocalSaleStatus.java) |

Esses domínios são enums do código; as migrações não criam `CHECK`/enum SQL para eles. Os índices de busca e filtros adicionais constam nas migrações, mas não mudam as cardinalidades. A presença de `references` no DDL do SQLite declara a relação; o código de conexão não habilita explicitamente `PRAGMA foreign_keys`, portanto não se deve presumir sua fiscalização em tempo de execução.
