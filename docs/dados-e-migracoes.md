# Dados e Migrações

O PostgreSQL é a base de dados central do Mercado One. O SQLite do PDV é um armazenamento local separado para operação offline parcial.

## PostgreSQL servidor

Configuração atual:

- Imagem local: `postgres:18`.
- Banco default: `mercado_one`.
- Usuário default: `mercado_one`.
- Porta default: `5432`.
- Volume local: `postgres18-data`.

A API lê as configurações por variáveis:

- `MERCADO_ONE_DATABASE_URL` (JDBC; default `jdbc:postgresql://localhost:5432/mercado_one`)
- `MERCADO_ONE_DB_USER`
- `MERCADO_ONE_DB_PASSWORD`
- `MERCADO_ONE_API_PORT`

`MERCADO_ONE_DB_NAME` e `MERCADO_ONE_DB_PORT` valem para o Compose. Se divergirem do default, a API só acompanha se `MERCADO_ONE_DATABASE_URL` for ajustada.

## Flyway

Flyway está habilitado no backend. As migrations atuais são:

- `V1__baseline.sql`: baseline controlada.
- `V2__access_and_catalog.sql`: usuários de acesso, categorias e produtos.
- `V3__inventory.sql`: saldos de estoque por produto e movimentações imutáveis.
- `V4__sales.sql`: vendas, itens e pagamentos.
- `V5__customers.sql`: clientes e vínculo opcional da venda ao cliente.
- `V6__suppliers_offline_audit.sql`: fornecedores, vínculo real opcional em movimentações de estoque e eventos de auditoria.
- `V7__offline_conflict_resolution.sql`: conflitos de venda offline, status de resolução e vínculo opcional com venda aceita.
- `V8__sales_discount_cancel_idempotency.sql`: total bruto e desconto da venda, `client_sale_id` único para idempotência, dados de cancelamento (`canceled_at`, `canceled_by_user_id`, `cancel_reason`) e índices de status da venda e de ação da auditoria.

`sales.status` persiste `CONFIRMED` ou `CANCELED`. `inventory_movements.type` persiste `ENTRY`, `ADJUSTMENT`, `SALE` ou `SALE_CANCELLATION`.

Diretrizes:

- Toda mudança de schema servidor deve entrar por migration Flyway.
- Migrations devem ser pequenas, revisáveis e ordenadas.
- Não depender de `ddl-auto` para criar schema em ambiente real.
- Evitar migrations destrutivas sem decisão explícita e plano de rollback.
- Dados de seed devem ser separados de estrutura quando crescerem em complexidade.
- O seed inicial de administrador é executado pela aplicação a partir de variáveis de ambiente, não por migration fixa.

## JPA

O Hibernate está configurado com:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
```

Isso significa que a aplicação valida o schema existente em vez de criá-lo automaticamente.

Os testes do backend não usam esse PostgreSQL. A maioria sobe H2 em memória, desliga o Flyway (`spring.flyway.enabled=false`) e deixa o Hibernate criar o schema (`ddl-auto=create-drop`). `PostgresMigrationTests` aplica as migrations Flyway num PostgreSQL via Testcontainers e é ignorado quando não há Docker.

## SQLite do PDV

O caminho padrão do banco local do PDV é:

```text
~/.mercado-one/pdv-offline.sqlite3
```

Uso atual:

- Catálogo local de produtos ativos e preços vigentes.
- Fila offline de vendas.
- Metadados de sincronização.

O SQLite local não deve virar fonte primária de cadastro. O servidor continua sendo a fonte de verdade para dados centrais.

Schema local atual do PDV:

- `offline_sales`: venda local, cliente opcional, desconto, status, venda remota quando sincronizada, erro e tentativas. A coluna `discount_amount` é adicionada na abertura do banco quando falta em arquivos antigos.
- `offline_sale_items`: itens da venda local com produto, quantidade e preço local.
- `offline_sale_payments`: pagamentos locais.
- `offline_sale_sync_attempts`: histórico simples de tentativas de sincronização.
- `local_catalog_products`: catálogo local de produtos ativos usado como fallback de busca e venda offline.

Status local da venda (`LocalSaleStatus`): `PENDING`, `SENT`, `CONFLICT`, `ERROR`, `REJECTED`. A resolução administrativa `ACCEPTED`/`REJECTED` vive na tabela servidor `offline_sale_conflicts`; o PDV a consulta por `GET /api/offline/sales/status` e grava `SENT` ou `REJECTED` na venda local.
