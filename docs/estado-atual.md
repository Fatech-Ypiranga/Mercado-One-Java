# Estado Atual

Este documento registra o estado real do Mercado One no scaffold atual. Ele deve ser atualizado sempre que uma fatia funcional deixar de ser apenas planejada e passar a existir no codigo.

## Visao geral

O projeto e um monorepo com quatro areas principais:

- `backend-api`: API REST em Spring Boot.
- `admin-web`: administrativo web em Angular.
- `pdv-desktop`: PDV desktop em JavaFX.
- `infra`: infraestrutura local de desenvolvimento.

O repositorio ainda nao esta em estado de MVP completo. Ele possui uma base executavel, contratos iniciais e fatias funcionais de acesso, catalogo, clientes, fornecedores, estoque simples, venda online, relatorios operacionais, fila offline do PDV, catalogo local, sincronizacao offline, conflitos resoluveis e auditoria inicial.

## Backend API

Implementado:

- Aplicacao Spring Boot 4.1.1 com Java 25.
- Dependencias de Web MVC, JPA, Validation, Security, Actuator, Flyway, PostgreSQL e H2 para testes.
- Configuracao de datasource por variaveis de ambiente.
- Migrations `V1__baseline.sql` a `V7__offline_conflict_resolution.sql` com usuarios, categorias, produtos, saldos, movimentacoes, vendas, itens, pagamentos, clientes, fornecedores, auditoria e conflitos offline.
- Envelope HTTP `ApiEnvelope`.
- Erro padronizado `ApiError`.
- `GlobalExceptionHandler` para validacao e erro inesperado.
- `SecurityConfig` com JWT Bearer, cookie HttpOnly para admin web, CORS local com credenciais, CSRF desabilitado, login publico, rotas protegidas e autorizacao por perfil para usuarios, catalogo, estoque, vendas, offline e fornecedores.
- Validacao de token consulta o usuario atual no banco para respeitar inativacao ou alteracao de perfil apos emissao do JWT.
- Seed de administrador inicial por variaveis de ambiente.
- CRUD administrativo inicial de usuarios e perfis.
- Enum `UserRole` com `ADMIN`, `GERENTE`, `OPERADOR_CAIXA` e `ESTOQUISTA`.
- `GET /api/system/info`.
- `POST /api/auth/login` e `GET /api/auth/me`.
- CRUD inicial de categorias e produtos.
- Estoque simples com saldo por produto, entrada, ajuste manual com justificativa, baixa por venda e movimentacoes imutaveis.
- Venda online em `POST /api/sales`, com cliente opcional, itens, pagamentos manuais e baixa de estoque.
- Relatorio de vendas em `GET /api/sales` por periodo, operador, cliente e status, com paginacao, totais e CSV.
- Relatorio de produtos mais vendidos em `GET /api/sales/top-products`.
- CRUD inicial de clientes com busca e status.
- CRUD inicial de fornecedores.
- Vinculo real opcional de fornecedor em entradas de estoque.
- Recebimento de venda offline por `POST /api/offline/sales/sync`.
- Resolucao manual de conflitos offline por endpoints administrativos.
- Relatorio de produtos mais vendidos por filtros de venda.
- Auditoria imutavel inicial para venda, estoque, usuarios e conflitos offline.

Nao implementado ainda:

- Refinamentos de UX para operacao piloto.

## Admin Web

Implementado:

- Aplicacao Angular 22 standalone.
- Login consumindo `POST /api/auth/login`.
- Sessao do admin por cookie HttpOnly, sem guardar JWT em `localStorage`.
- Shell com barra lateral protegido por guard de autenticacao e perfil.
- Rotas para inicio, usuarios, produtos, categorias, estoque, vendas, conflitos offline, clientes e fornecedores.
- Dashboard inicial consumindo `GET /api/system/info` e listagens permitidas para o perfil.
- Telas de usuarios, categorias, produtos, estoque, vendas e clientes com filtros, periodo de movimentacoes, formularios e estados de loading, vazio, erro e sucesso.
- Tela de fornecedores, relatorio de vendas com totais, paginacao, CSV e produtos mais vendidos.
- Tela de conflitos offline com aceite ou rejeicao manual.
- Tipos TypeScript para envelope, erro, system info, auth, catalogo, estoque, vendas, clientes, fornecedores e offline.

Nao implementado ainda:

- Padronizacao completa de mensagens em todas as telas.

## PDV Desktop

Implementado:

- Aplicacao JavaFX.
- Tela inicial com status de sincronizacao, login de operador, busca online de produtos/clientes, carrinho e finalizacao com persistencia local antes do envio.
- `SyncStatus` com estados iniciais de exibicao.
- `OfflineStorageConfig` com caminho padrao para SQLite local.
- Cliente HTTP para login, busca online e sincronizacao de venda offline na API.
- Fila SQLite local para preservar venda antes da tentativa de envio.
- Catalogo local SQLite para produtos ativos e precos vigentes, usado como fallback quando a API nao responde.
- Comprovante simples nao fiscal apos finalizacao.
- Estados locais `PENDING`, `SENT`, `CONFLICT` e `ERROR`.

Nao implementado ainda:

- Refinamentos de UX para resolucao assistida de conflitos no PDV.

## Infra

Implementado:

- `docker-compose.yml` com PostgreSQL 18.
- Variaveis default para banco, usuario, senha, porta e porta da API.
- Volume persistente local.
- Healthcheck do PostgreSQL.

## Verificacao conhecida

Existem testes minimos nos tres subprojetos. No ambiente usado para esta atualizacao:

- `backend-api`: `mvn test` passou com 33 testes.
- `backend-api`: `mvn spring-boot:run` subiu a API local com `.env`, aplicou Flyway V1/V2 e autenticou o admin inicial.
- `admin-web`: `node node_modules\\typescript\\bin\\tsc -p tsconfig.app.json --noEmit` passou.
- `admin-web`: `node node_modules\\typescript\\bin\\tsc -p tsconfig.spec.json --noEmit` passou.
- `admin-web`: `node node_modules\\@angular\\compiler-cli\\bundles\\src\\bin\\ngc.js -p tsconfig.app.json --noEmit` passou.
- `admin-web`: `node node_modules\\@angular\\compiler-cli\\bundles\\src\\bin\\ngc.js -p tsconfig.spec.json --noEmit` passou.
- `admin-web`: `npm test -- --watch=false --progress=false` passou com 56 specs usando `ChromeHeadlessNoGpu`.
- `admin-web`: `npm run build -- --configuration development --progress=false --clear-screen=false` passou e gerou artefatos em `output/admin-web-dist`.
- `pdv-desktop`: `mvn test` passou com 8 testes.
