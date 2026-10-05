# Testes e Verificação

Este documento registra os checks esperados do Mercado One e o estado conhecido da suíte atual.

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

O script `npm test` em `admin-web/package.json` já passa `--watch=false` e usa o hook `scripts/angular-karma-output-hook.cjs`. O browser é `ChromeHeadlessNoGpu` (`admin-web/karma.conf.cjs`).
Para medir cobertura no PowerShell, execute `$env:MERCADO_ONE_COVERAGE='1'; npm test -- --code-coverage`. O relatório aplica o mínimo de 80% a statements e linhas; os arquivos são gravados em `output/admin-web-test-out/output/admin-web-coverage/`.
Os testes Playwright em `admin-web/e2e/` exercitam login, navegação por perfil, cadastro de categoria, entrada de estoque e confirmação de conflito em Chromium desktop e celular. Eles interceptam a API com respostas determinísticas: verificam a jornada do navegador e o corpo enviado, mas não substituem uma verificação integrada com backend e banco. Inicie `npm start` em outro terminal antes de `npm run test:e2e`. `MERCADO_ONE_E2E_BASE_URL` pode apontar para outro admin web de teste já iniciado; `MERCADO_ONE_E2E_LOGIN` e `MERCADO_ONE_E2E_PASSWORD` definem os dados usados no formulário de teste.

PDV desktop:

```bash
cd pdv-desktop
mvn test
```

## Testes existentes

- Backend: teste de contexto da aplicação com H2 em memória, `spring.flyway.enabled=false` e schema gerado pelo contexto de teste.
- Backend: testes de login, `/api/auth/me` e proteção de rota sem token.
- Backend: testes de perfis, criação, atualização, filtros, validação de usuários, bloqueio de operador em gestão de usuários e revalidação de role atual para token antigo.
- Backend: testes de criação, atualização e filtros de categorias e produtos.
- Backend: testes de entrada, ajuste, validação, filtros principais de estoque e baixa por venda.
- Backend: testes de venda confirmada via `POST /api/sales`, cliente opcional, consulta por cliente, rejeição por estoque insuficiente e arredondamento monetário em item fracionado.
- Backend: testes de criação, atualização, filtros e autorização básica de clientes.
- Backend: teste de `GET /api/system/info` validando envelope e roles.
- Backend: testes de fornecedores, sincronização offline, resolução manual de conflitos offline e produtos mais vendidos.
- Admin web: testes de raiz/shell, `AuthService` (cookie/`sessionStorage`, sem JWT em `localStorage`), `AccessService`, `InventoryService`, `CustomerService`, `SalesService`, `CatalogService`, `SupplierService`, `OfflineService`, interceptor com `withCredentials`, validação/erro do login, timeout de requisição sem resposta, dashboard, vendas e formulários/páginas de usuários, estoque, categorias, produtos, clientes, fornecedores e conflitos offline.
- PDV desktop: teste de `OfflineStorageConfig`, fila offline SQLite, catálogo local SQLite e cliente HTTP (login, consultas e corpo de `POST /api/sales` / sync).

## Estado de verificação desta atualização

Na refatoração do admin web, `npm test -- --code-coverage` passou com 71 specs: 80,94% de statements, 85,29% de linhas, 75,17% de funções e 62,65% de branches. `npm run build` passou com bundle inicial de 349,26 kB (orçamento de 500 kB), `npm run test:e2e` passou com 10 cenários em Chromium desktop e celular e `npm audit` encontrou 0 vulnerabilidades. Os testes de backend e PDV não foram executados nesta refatoração de interface.

## Diretrizes

- Para correções de bugs, adicionar teste de regressão quando for prático.
- Para nova fatia de backend, testar regra de domínio e contrato HTTP principal.
- Para nova tela Angular, testar renderização básica, integração com service e estados de erro relevantes.
- Para PDV/offline, testar preservação local da venda antes de testar sincronização.
- Não remover ou enfraquecer teste para obter build verde sem entender a causa.
