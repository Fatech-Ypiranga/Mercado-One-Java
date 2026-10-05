# Estado Atual

Este documento registra o estado real do Mercado One no scaffold atual. Ele deve ser atualizado sempre que uma fatia funcional deixar de ser apenas planejada e passar a existir no código.

## Visão geral

O projeto é um monorepo com quatro áreas principais:

- `backend-api`: API REST em Spring Boot.
- `admin-web`: administrativo web em Angular.
- `pdv-desktop`: PDV desktop em JavaFX.
- `infra`: infraestrutura local de desenvolvimento.

O repositório ainda não está em estado de MVP completo. Ele possui uma base executável, contratos HTTP e fatias funcionais de acesso, catálogo, clientes, fornecedores, estoque simples, venda confirmada no servidor, relatórios operacionais, fila offline do PDV, catálogo local, sincronização offline, conflitos resolúveis e auditoria inicial (gravação).

## Backend API

Implementado:

- Aplicação Spring Boot 4.1.1 com Java 25. Artefato `0.1.0-SNAPSHOT`.
- Dependências de Web MVC, JPA, Validation, Security, Actuator, Flyway, PostgreSQL e H2 para testes.
- Configuração de datasource por variáveis de ambiente. `application.yml` importa `.env` opcional do diretório atual ou da raiz do repositório.
- `MERCADO_ONE_JWT_SECRET` obrigatório em runtime; sem ele o Spring falha no placeholder.
- Migrations `V1__baseline.sql` a `V7__offline_conflict_resolution.sql` com usuários, categorias, produtos, saldos, movimentações, vendas, itens, pagamentos, clientes, fornecedores, auditoria e conflitos offline.
- Envelope HTTP `ApiEnvelope`.
- Erro padronizado `ApiError`.
- `GlobalExceptionHandler` para validação, credencial inválida, entidade ausente, regra de negócio e erro inesperado.
- `SecurityConfig` com JWT Bearer, cookie HttpOnly `mercado_one_admin_session` para admin web, CORS com credenciais a partir de `MERCADO_ONE_CORS_ALLOWED_ORIGINS` (default local `localhost:4200` e `127.0.0.1:4200`), CSRF desabilitado, login público, rotas protegidas e autorização por perfil. Cookie `Secure` e `SameSite=None` só com `MERCADO_ONE_COOKIE_SECURE=true`.
- Validação de token consulta o usuário atual no banco para respeitar inativação ou alteração de perfil após emissão do JWT.
- Seed de administrador inicial por variáveis de ambiente (`admin` / `admin123` no `.env.example`).
- CRUD administrativo inicial de usuários e perfis, inclusive senha opcional na atualização.
- Enum `UserRole` com `ADMIN`, `GERENTE`, `OPERADOR_CAIXA` e `ESTOQUISTA`.
- `GET /api/system/info` público.
- `POST /api/auth/login`, `POST /api/auth/logout` e `GET /api/auth/me`.
- CRUD inicial de categorias e produtos, com busca por nome, SKU e código de barras e campos fiscais preparatórios.
- Estoque simples com saldo por produto, entrada de um produto por request, ajuste manual com justificativa, baixa por venda e movimentações imutáveis.
- `POST /api/sales` confirma venda com preço vigente do servidor, cliente opcional, itens, pagamentos manuais e baixa de estoque. Status persistido: apenas `CONFIRMED`.
- Relatório de vendas em `GET /api/sales` por período, operador, cliente e status, com paginação, totais e CSV.
- Relatório de produtos mais vendidos em `GET /api/sales/top-products`.
- CRUD inicial de clientes com busca administrativa, busca operacional (`GET /api/customers/search`) e status. Histórico de compras e `GET /api/sales?customerId=`.
- CRUD inicial de fornecedores.
- Vínculo real opcional de fornecedor em entradas de estoque.
- Recebimento de venda offline por `POST /api/offline/sales/sync`.
- Resolução manual de conflitos offline por endpoints administrativos.
- Auditoria imutável inicial em `audit_events` para venda, estoque, usuários e conflitos offline. Não há endpoint de consulta.

Não implementado ainda:

- Consulta administrativa de auditoria.
- Auditoria de criação/alteração de produto (requisito conceitual; o módulo `catalog` não grava evento).
- Cancelamento pós-venda (`SaleStatus` só tem `CONFIRMED`).
- Desconto simples.
- Entrada de estoque como documento com vários itens em um único POST.
- Refinamentos de UX para operação piloto.

## Admin Web

Implementado:

- Aplicação Angular 22 standalone, prefixo `mo`, `npm start` em `0.0.0.0:4200`.
- Login consumindo `POST /api/auth/login` com `withCredentials`.
- Sessão: cookie HttpOnly na API; metadados (`expiresAt`, `user`) em `sessionStorage` na chave `mercado-one-admin-session-state`. O JWT não é guardado em `localStorage`.
- Shell com barra lateral protegido por guard de autenticação e perfil.
- Rotas para início, usuários, produtos, categorias, estoque, vendas, conflitos offline, clientes e fornecedores.
- Dashboard inicial com briefing do dia (conflitos pendentes e totais de venda para `ADMIN`/`GERENTE`), versão da API e atalhos por departamento. `ESTOQUISTA` vê o início, mas não os totais de venda nem conflitos.
- Telas de usuários, categorias, produtos, estoque, vendas, conflitos offline, clientes e fornecedores com filtros, período de movimentações, formulários e estados de loading, vazio, erro e sucesso.
- Histórico de cliente: link `/vendas?customerId=` na tela de clientes.
- Identidade visual de painel operacional claro, com IBM Plex, superfícies neutras, verde para ação principal e navegação por departamento recolhível em telas menores.
- Services tipados: `ApiClientService`, `AuthService`, `AccessService`, `CatalogService`, `InventoryService`, `SalesService`, `CustomerService`, `SupplierService` e `OfflineService`.
- Ambiente local em `src/environments/environment.ts` apontando para `http://localhost:8080`. O build de produção lê `MERCADO_ONE_API_BASE_URL` via `npm run build:production`. `npm run build` deixa essa URL vazia.
- Piloto Azure descrito em [infra/README.md](../infra/README.md).
- Build gera artefatos em `output/admin-web-dist` (ignorado pelo git).

Não implementado ainda:

- Telas para `OPERADOR_CAIXA` no admin web.
- Padronização completa de mensagens em todas as telas.
- Uso efetivo de `SaleStatus.CANCELED` (o tipo TypeScript declara o valor; o backend e o filtro da tela de vendas só conhecem `CONFIRMED`).

## PDV Desktop

Implementado:

- Aplicação JavaFX 25 (`com.mercadoone.pdv.PdvDesktopApplication`).
- Tela única de venda presencial: API URL, login, busca de produtos/clientes, carrinho, pagamento único, comprovante textual e feedback.
- `OfflineStorageConfig` com caminho padrão `~/.mercado-one/pdv-offline.sqlite3`.
- Cliente HTTP para login (`POST /api/auth/login`), busca de produtos (`GET /api/catalog/products`), busca operacional de clientes (`GET /api/customers/search`) e sync (`POST /api/offline/sales/sync`).
- `OnlineSaleClient.finalizeSale` monta `POST /api/sales` e é coberto por teste; a UI não chama esse método.
- Fila SQLite local com estados `PENDING`, `SENT`, `CONFLICT` e `ERROR`.
- Catálogo local SQLite para produtos ativos e preços vigentes, usado como fallback quando a API não responde ou não há token.
- Comprovante simples não fiscal após finalização (antes da resposta da API).
- No login, reenvio de vendas `PENDING` e `ERROR`. `CONFLICT` não é reenviado.
- Resolução de conflito no admin não muda o status local da venda no PDV.

Não implementado ainda:

- Chamada da UI a `POST /api/sales`.
- Badge `SyncStatus` dinâmico: o enum existe (`ONLINE`, `OFFLINE_READY`, `SYNC_PENDING`, `SYNC_FAILED`), mas o cabeçalho permanece no valor inicial `OFFLINE_READY`.
- Pagamento múltiplo na mesma venda (a API aceita lista; a UI envia um pagamento).
- Cadastro de cliente no PDV, desconto, cancelamento pós-venda.
- Atualização local quando o admin aceita ou rejeita um conflito.
- Refinamentos de UX para resolução assistida de conflitos no PDV.

## Infra

Implementado:

- `infra/docker-compose.yml` com PostgreSQL 18.
- Container `mercado-one-postgres`, volume `postgres18-data`, healthcheck via `pg_isready`.
- Defaults de banco, usuário, senha e porta iguais aos de `.env.example`.
- Compose interpola `${MERCADO_ONE_*}` a partir do diretório do compose (`infra/`) ou do ambiente do shell; o `.env` da raiz é lido pela API, não automaticamente pelo Compose.

## Verificação conhecida

Existem testes mínimos nos três subprojetos. Contagem no código-fonte:

- `backend-api`: 33 métodos `@Test`.
- `admin-web`: 56 specs `it(`, Karma `ChromeHeadlessNoGpu`.
- `pdv-desktop`: 8 métodos `@Test`.

A última execução registrada na documentação passou com esses totais. Esta atualização de documentação não reexecutou a suíte e não alterou código de aplicação.
