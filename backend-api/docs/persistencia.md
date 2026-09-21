# Persistencia do Backend

O backend usa PostgreSQL como banco central e Flyway como mecanismo de migracao.

## Configuracao atual

Arquivo: `src/main/resources/application.yml`.

- Porta da API: `MERCADO_ONE_API_PORT`, default `8080`.
- URL JDBC: `MERCADO_ONE_DATABASE_URL`, default `jdbc:postgresql://localhost:5432/mercado_one`.
- Usuario: `MERCADO_ONE_DB_USER`, default `mercado_one`.
- Senha: `MERCADO_ONE_DB_PASSWORD`, default `mercado_one_dev`.
- Em desenvolvimento local, `application.yml` importa `.env` opcional do diretorio atual ou da raiz do repositorio. `MERCADO_ONE_JWT_SECRET` continua obrigatorio.
- `MERCADO_ONE_DB_NAME` e `MERCADO_ONE_DB_PORT` sao do Compose; se mudarem, ajuste `MERCADO_ONE_DATABASE_URL`.
- Flyway habilitado em runtime. Testes desligam Flyway e usam H2 com `ddl-auto=create-drop`.
- Hibernate com `ddl-auto=validate`.
- `open-in-view=false`.

## Migrations atuais

- `src/main/resources/db/migration/V1__baseline.sql`: baseline controlada.
- `src/main/resources/db/migration/V2__access_and_catalog.sql`: usuarios, categorias e produtos.
- `src/main/resources/db/migration/V3__inventory.sql`: saldos e movimentacoes de estoque.
- `src/main/resources/db/migration/V4__sales.sql`: vendas, itens e pagamentos.
- `src/main/resources/db/migration/V5__customers.sql`: clientes e vinculo opcional da venda ao cliente.
- `src/main/resources/db/migration/V6__suppliers_offline_audit.sql`: fornecedores, vinculo real opcional em movimentacoes de estoque e eventos de auditoria.
- `src/main/resources/db/migration/V7__offline_conflict_resolution.sql`: conflitos de venda offline, status de resolucao e vinculo opcional com venda aceita.

## Diretrizes para novas tabelas

- A tabela deve pertencer claramente a um modulo.
- Nomear constraints e indices quando isso ajudar manutencao.
- Registrar datas e usuarios responsaveis nas operacoes criticas previstas pelos requisitos.
- Evitar acoplar o schema a multi-loja antes da necessidade real, mas tambem nao impedir evolucao futura sem motivo.
- Escrever testes de repository/integracao quando a persistencia carregar regra relevante.
