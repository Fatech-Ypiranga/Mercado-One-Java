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
npm run build
npm run test:e2e
```

O script `npm test` em `admin-web/package.json` ja passa `--watch=false` e usa o hook `scripts/angular-karma-output-hook.cjs`. O browser e `ChromeHeadlessNoGpu` (`admin-web/karma.conf.cjs`).
Para medir cobertura no PowerShell, execute `$env:MERCADO_ONE_COVERAGE='1'; npm test -- --code-coverage`. O relatorio aplica minimo de 80% a statements e linhas; os arquivos sao gravados em `output/admin-web-test-out/output/admin-web-coverage/`.
Os testes Playwright em `admin-web/e2e/` exercitam login, navegacao por perfil, cadastro de categoria, entrada de estoque e confirmacao de conflito em Chromium desktop e celular. Eles interceptam a API com respostas deterministicas: verificam a jornada do navegador e o corpo enviado, mas nao substituem uma verificacao integrada com backend e banco. Inicie `npm start` em outro terminal antes de `npm run test:e2e`. `MERCADO_ONE_E2E_BASE_URL` pode apontar para outro admin web de teste ja iniciado; `MERCADO_ONE_E2E_LOGIN` e `MERCADO_ONE_E2E_PASSWORD` definem os dados usados no formulario de teste.

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
- Admin web: testes de raiz/shell, `AuthService` (cookie/`sessionStorage`, sem JWT em `localStorage`), `AccessService`, `InventoryService`, `CustomerService`, `SalesService`, `CatalogService`, `SupplierService`, `OfflineService`, interceptor com `withCredentials`, validacao/erro do login, timeout de requisicao sem resposta, dashboard, vendas e formularios/paginas de usuarios, estoque, categorias, produtos, clientes, fornecedores e conflitos offline.
- PDV desktop: teste de `OfflineStorageConfig`, fila offline SQLite, catalogo local SQLite e cliente HTTP (login, consultas e corpo de `POST /api/sales` / sync).

## Estado de verificacao desta atualizacao

Na refatoracao do admin web, `npm test -- --code-coverage` passou com 71 specs: 80,94% de statements, 85,29% de linhas, 75,17% de funcoes e 62,65% de branches. `npm run build` passou com bundle inicial de 349,26 kB (orcamento de 500 kB), `npm run test:e2e` passou com 10 cenarios em Chromium desktop e celular e `npm audit` encontrou 0 vulnerabilidades. Os testes de backend e PDV nao foram executados nesta refatoracao de interface.

## Diretrizes

- Para bug fix, adicionar teste de regressao quando pratico.
- Para nova fatia de backend, testar regra de dominio e contrato HTTP principal.
- Para nova tela Angular, testar renderizacao basica, integracao com service e estados de erro relevantes.
- Para PDV/offline, testar preservacao local da venda antes de testar sincronizacao.
- Nao remover ou enfraquecer teste para obter build verde sem entender a causa.
