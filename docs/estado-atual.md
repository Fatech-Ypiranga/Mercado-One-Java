# Estado Atual

Este documento registra o estado real do Mercado One no scaffold atual. Ele deve ser atualizado sempre que uma fatia funcional deixar de ser apenas planejada e passar a existir no codigo.

## Visao geral

O projeto e um monorepo com quatro areas principais:

- `backend-api`: API REST em Spring Boot.
- `admin-web`: administrativo web em Angular.
- `pdv-desktop`: PDV desktop em JavaFX.
- `infra`: infraestrutura local de desenvolvimento.

O repositorio ainda nao esta em estado de MVP completo. Ele possui uma base executavel, contratos HTTP e fatias funcionais de acesso, catalogo, clientes, fornecedores, estoque simples, venda confirmada no servidor, relatorios operacionais, fila offline do PDV, catalogo local, sincronizacao offline, conflitos resoluveis e auditoria inicial (gravacao).

## Backend API

Implementado:

- Aplicacao Spring Boot 4.1.1 com Java 25. Artefato `0.1.0-SNAPSHOT`.
- Dependencias de Web MVC, JPA, Validation, Security, Actuator, Flyway, PostgreSQL e H2 para testes.
- Configuracao de datasource por variaveis de ambiente. `application.yml` importa `.env` opcional do diretorio atual ou da raiz do repositorio.
- `MERCADO_ONE_JWT_SECRET` obrigatorio em runtime; sem ele o Spring falha no placeholder.
- Migrations `V1__baseline.sql` a `V7__offline_conflict_resolution.sql` com usuarios, categorias, produtos, saldos, movimentacoes, vendas, itens, pagamentos, clientes, fornecedores, auditoria e conflitos offline.
- Envelope HTTP `ApiEnvelope`.
- Erro padronizado `ApiError`.
- `GlobalExceptionHandler` para validacao, credencial invalida, entidade ausente, regra de negocio e erro inesperado.
- `SecurityConfig` com JWT Bearer, cookie HttpOnly `mercado_one_admin_session` para admin web, CORS local com credenciais para `localhost:4200` e `127.0.0.1:4200`, CSRF desabilitado, login publico, rotas protegidas e autorizacao por perfil.
- Validacao de token consulta o usuario atual no banco para respeitar inativacao ou alteracao de perfil apos emissao do JWT.
- Seed de administrador inicial por variaveis de ambiente (`admin` / `admin123` no `.env.example`).
- CRUD administrativo inicial de usuarios e perfis, inclusive senha opcional na atualizacao.
- Enum `UserRole` com `ADMIN`, `GERENTE`, `OPERADOR_CAIXA` e `ESTOQUISTA`.
- `GET /api/system/info` publico.
- `POST /api/auth/login`, `POST /api/auth/logout` e `GET /api/auth/me`.
- CRUD inicial de categorias e produtos, com busca por nome, SKU e codigo de barras e campos fiscais preparatorios.
- Estoque simples com saldo por produto, entrada de um produto por request, ajuste manual com justificativa, baixa por venda e movimentacoes imutaveis.
- `POST /api/sales` confirma venda com preco vigente do servidor, cliente opcional, itens, pagamentos manuais e baixa de estoque. Status persistido: apenas `CONFIRMED`.
- Relatorio de vendas em `GET /api/sales` por periodo, operador, cliente e status, com paginacao, totais e CSV.
- Relatorio de produtos mais vendidos em `GET /api/sales/top-products`.
- CRUD inicial de clientes com busca administrativa, busca operacional (`GET /api/customers/search`) e status. Historico de compras e `GET /api/sales?customerId=`.
- CRUD inicial de fornecedores.
- Vinculo real opcional de fornecedor em entradas de estoque.
- Recebimento de venda offline por `POST /api/offline/sales/sync`.
- Resolucao manual de conflitos offline por endpoints administrativos.
- Auditoria imutavel inicial em `audit_events` para venda, estoque, usuarios e conflitos offline. Nao ha endpoint de consulta.

Nao implementado ainda:

- Consulta administrativa de auditoria.
- Auditoria de criacao/alteracao de produto (requisito conceitual; o modulo `catalog` nao grava evento).
- Cancelamento pos-venda (`SaleStatus` so tem `CONFIRMED`).
- Desconto simples.
- Entrada de estoque como documento com varios itens em um unico POST.
- Refinamentos de UX para operacao piloto.

## Admin Web

Implementado:

- Aplicacao Angular 22 standalone, prefixo `mo`, `npm start` em `0.0.0.0:4200`.
- Login consumindo `POST /api/auth/login` com `withCredentials`.
- Sessao: cookie HttpOnly na API; metadados (`expiresAt`, `user`) em `sessionStorage` na chave `mercado-one-admin-session-state`. O JWT nao e guardado em `localStorage`.
- Shell com barra lateral protegido por guard de autenticacao e perfil.
- Rotas para inicio, usuarios, produtos, categorias, estoque, vendas, conflitos offline, clientes e fornecedores.
- Dashboard inicial com briefing do dia (conflitos pendentes e totais de venda para `ADMIN`/`GERENTE`), versao da API e atalhos por departamento. `ESTOQUISTA` ve o inicio, mas nao os totais de venda nem conflitos.
- Telas de usuarios, categorias, produtos, estoque, vendas, conflitos offline, clientes e fornecedores com filtros, periodo de movimentacoes, formularios e estados de loading, vazio, erro e sucesso.
- Historico de cliente: link `/vendas?customerId=` na tela de clientes.
- Identidade visual de balcao: papel de talao, Petrona e IBM Plex, navegacao por departamento.
- Services tipados: `ApiClientService`, `AuthService`, `AccessService`, `CatalogService`, `InventoryService`, `SalesService`, `CustomerService`, `SupplierService` e `OfflineService`.
- Ambiente local em `src/environments/environment.ts` apontando para `http://localhost:8080`. `environment.prod.ts` usa `apiBaseUrl` vazio (same-origin); nao ha deploy de producao documentado.
- Build gera artefatos em `output/admin-web-dist` (ignorado pelo git).

Nao implementado ainda:

- Telas para `OPERADOR_CAIXA` no admin web.
- Padronizacao completa de mensagens em todas as telas.
- Uso efetivo de `SaleStatus.CANCELED` (o tipo TypeScript declara o valor; o backend e o filtro da tela de vendas so conhecem `CONFIRMED`).

## PDV Desktop

Implementado:

- Aplicacao JavaFX 25 (`com.mercadoone.pdv.PdvDesktopApplication`).
- Tela unica de venda presencial: API URL, login, busca de produtos/clientes, carrinho, pagamento unico, comprovante textual e feedback.
- `OfflineStorageConfig` com caminho padrao `~/.mercado-one/pdv-offline.sqlite3`.
- Cliente HTTP para login (`POST /api/auth/login`), busca de produtos (`GET /api/catalog/products`), busca operacional de clientes (`GET /api/customers/search`) e sync (`POST /api/offline/sales/sync`).
- `OnlineSaleClient.finalizeSale` monta `POST /api/sales` e e coberto por teste; a UI nao chama esse metodo.
- Fila SQLite local com estados `PENDING`, `SENT`, `CONFLICT` e `ERROR`.
- Catalogo local SQLite para produtos ativos e precos vigentes, usado como fallback quando a API nao responde ou nao ha token.
- Comprovante simples nao fiscal apos finalizacao (antes da resposta da API).
- No login, reenvio de vendas `PENDING` e `ERROR`. `CONFLICT` nao e reenviado.
- Resolucao de conflito no admin nao muda o status local da venda no PDV.

Nao implementado ainda:

- Chamada da UI a `POST /api/sales`.
- Badge `SyncStatus` dinamico: o enum existe (`ONLINE`, `OFFLINE_READY`, `SYNC_PENDING`, `SYNC_FAILED`), mas o cabecalho permanece no valor inicial `OFFLINE_READY`.
- Pagamento multiplo na mesma venda (a API aceita lista; a UI envia um pagamento).
- Cadastro de cliente no PDV, desconto, cancelamento pos-venda.
- Atualizacao local quando o admin aceita ou rejeita um conflito.
- Refinamentos de UX para resolucao assistida de conflitos no PDV.

## Infra

Implementado:

- `infra/docker-compose.yml` com PostgreSQL 18.
- Container `mercado-one-postgres`, volume `postgres18-data`, healthcheck via `pg_isready`.
- Defaults de banco, usuario, senha e porta iguais aos de `.env.example`.
- Compose interpola `${MERCADO_ONE_*}` a partir do diretorio do compose (`infra/`) ou do ambiente do shell; o `.env` da raiz e lido pela API, nao automaticamente pelo Compose.

## Verificacao conhecida

Existem testes minimos nos tres subprojetos. Contagem no codigo-fonte:

- `backend-api`: 33 metodos `@Test`.
- `admin-web`: 56 specs `it(`, Karma `ChromeHeadlessNoGpu`.
- `pdv-desktop`: 8 metodos `@Test`.

A ultima execucao registrada na documentacao passou com esses totais. Esta atualizacao de documentacao nao reexecutou a suite e nao alterou codigo de aplicacao.
