# Testes e Verificacao

Este documento registra os checks esperados do Mercado One e o estado conhecido da suite atual.

## Checks esperados

Backend:

```bash
cd backend-api
mvn test
```

Admin web:

```bash
cd admin-web
npm test -- --watch=false
```

O script `npm test` em `admin-web/package.json` ja passa `--watch=false` e usa o hook `scripts/angular-karma-output-hook.cjs`. O browser e `ChromeHeadlessNoGpu` (`admin-web/karma.conf.cjs`).

PDV desktop:

```bash
cd pdv-desktop
mvn test
```

## Testes existentes

- Backend: teste de contexto da aplicacao com H2 em memoria, `spring.flyway.enabled=false` e schema gerado pelo contexto de teste.
- Backend: testes de login, `/api/auth/me` e protecao de rota sem token.
- Backend: testes de perfis, criacao, atualizacao, filtros, validacao de usuarios, bloqueio de operador em gestao de usuarios e revalidacao de role atual para token antigo.
- Backend: testes de criacao, atualizacao e filtros de categorias e produtos.
- Backend: testes de entrada, ajuste, validacao, filtros principais de estoque e baixa por venda.
- Backend: testes de venda confirmada via `POST /api/sales`, cliente opcional, consulta por cliente, rejeicao por estoque insuficiente e arredondamento monetario em item fracionado.
- Backend: testes de criacao, atualizacao, filtros e autorizacao basica de clientes.
- Backend: teste de `GET /api/system/info` validando envelope e roles.
- Backend: testes de fornecedores, sincronizacao offline, resolucao manual de conflitos offline e produtos mais vendidos.
- Admin web: testes de raiz/shell, `AuthService` (cookie/`sessionStorage`, sem JWT em `localStorage`), `AccessService`, `InventoryService`, `CustomerService`, `SalesService`, `CatalogService`, `SupplierService`, `OfflineService`, interceptor com `withCredentials`, validacao/erro do login, timeout de requisicao sem resposta e formularios/paginas de usuarios, estoque, categorias, produtos, clientes, fornecedores e conflitos offline.
- PDV desktop: teste de `OfflineStorageConfig`, fila offline SQLite, catalogo local SQLite e cliente HTTP (login, consultas e corpo de `POST /api/sales` / sync).

## Estado de verificacao desta atualizacao

Contagem no codigo-fonte, sem reexecutar a suite:

- `backend-api`: 33 metodos `@Test`.
- `admin-web`: 56 specs `it(`.
- `pdv-desktop`: 8 metodos `@Test`.

A ultima execucao registrada na documentacao passou com esses totais (`ChromeHeadlessNoGpu` no admin). Esta atualizacao de documentacao nao altera codigo de aplicacao, contratos, migrations ou testes, e nao reexecutou Maven/npm.

## Diretrizes

- Para bug fix, adicionar teste de regressao quando pratico.
- Para nova fatia de backend, testar regra de dominio e contrato HTTP principal.
- Para nova tela Angular, testar renderizacao basica, integracao com service e estados de erro relevantes.
- Para PDV/offline, testar preservacao local da venda antes de testar sincronizacao.
- Nao remover ou enfraquecer teste para obter build verde sem entender a causa.
