# Dados e Migracoes

O PostgreSQL e a base de dados central do Mercado One. O SQLite do PDV e um armazenamento local separado para operacao offline parcial.

## PostgreSQL servidor

Configuracao atual:

- Imagem local: `postgres:18`.
- Banco default: `mercado_one`.
- Usuario default: `mercado_one`.
- Porta default: `5432`.
- Volume local: `postgres18-data`.

A API le as configuracoes por variaveis:

- `MERCADO_ONE_DATABASE_URL`
- `MERCADO_ONE_DB_USER`
- `MERCADO_ONE_DB_PASSWORD`

## Flyway

Flyway esta habilitado no backend. As migrations atuais sao:

- `V1__baseline.sql`: baseline controlada.
- `V2__access_and_catalog.sql`: usuarios de acesso, categorias e produtos.
- `V3__inventory.sql`: saldos de estoque por produto e movimentacoes imutaveis.
- `V4__sales.sql`: vendas online, itens e pagamentos.
- `V5__customers.sql`: clientes e vinculo opcional da venda ao cliente.
- `V6__suppliers_offline_audit.sql`: fornecedores, vinculo real opcional em movimentacoes de estoque e eventos de auditoria.
- `V7__offline_conflict_resolution.sql`: conflitos de venda offline, status de resolucao e vinculo opcional com venda aceita.

Diretrizes:

- Toda mudanca de schema servidor deve entrar por migration Flyway.
- Migrations devem ser pequenas, revisaveis e ordenadas.
- Nao depender de `ddl-auto` para criar schema em ambiente real.
- Evitar migrations destrutivas sem decisao explicita e plano de rollback.
- Dados de seed devem ser separados de estrutura quando crescerem em complexidade.
- O seed inicial de administrador e executado pela aplicacao a partir de variaveis de ambiente, nao por migration fixa.

## JPA

O Hibernate esta configurado com:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
```

Isso significa que a aplicacao valida o schema existente em vez de cria-lo automaticamente.

## SQLite do PDV

O caminho padrao planejado para o banco local do PDV e:

```text
~/.mercado-one/pdv-offline.sqlite3
```

Uso atual:

- Catalogo local de produtos ativos e precos vigentes.
- Fila offline de vendas.
- Metadados de sincronizacao.

O SQLite local nao deve virar fonte primaria de cadastro. O servidor continua sendo a fonte de verdade para dados centrais.

Schema local atual do PDV:

- `offline_sales`: venda local, cliente opcional, status, venda remota quando sincronizada, erro e tentativas.
- `offline_sale_items`: itens da venda local com produto, quantidade e preco local.
- `offline_sale_payments`: pagamentos locais.
- `offline_sale_sync_attempts`: historico simples de tentativas de sincronizacao.
- `local_catalog_products`: catalogo local de produtos ativos usado como fallback de busca e venda offline.
