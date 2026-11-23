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

Se o Karma falhar com `Cannot start ChromeHeadless` (Chrome fora do caminho padrão), aponte `CHROME_BIN` para qualquer Chromium, por exemplo o do Playwright: `$env:CHROME_BIN = "$env:LOCALAPPDATA\ms-playwright\chromium-1248\chrome-win64\chrome.exe"` (o número da pasta muda com a versão instalada).

O script `npm test` em `admin-web/package.json` já passa `--watch=false` e usa o hook `scripts/angular-karma-output-hook.cjs`. O browser é `ChromeHeadlessNoGpu` (`admin-web/karma.conf.cjs`).
Para medir cobertura no PowerShell, execute `$env:MERCADO_ONE_COVERAGE='1'; npm test -- --code-coverage`. O relatório aplica o mínimo de 80% a statements e linhas; os arquivos são gravados em `output/admin-web-test-out/output/admin-web-coverage/`.
Os testes Playwright em `admin-web/e2e/` exercitam login, navegação por perfil, cadastro de categoria, entrada de estoque e confirmação de conflito em Chromium desktop e celular. Eles interceptam a API com respostas determinísticas: verificam a jornada do navegador e o corpo enviado, mas não substituem uma verificação integrada com backend e banco. Inicie `npm start` em outro terminal antes de `npm run test:e2e`. `MERCADO_ONE_E2E_BASE_URL` pode apontar para outro admin web de teste já iniciado; `MERCADO_ONE_E2E_LOGIN` e `MERCADO_ONE_E2E_PASSWORD` definem os dados usados no formulário de teste.

PDV desktop:

```bash
cd pdv-desktop
mvn test
```

Os testes do PDV cobrem fila offline, catálogo local e checkout; a interface JavaFX (tema, fonte, layout) não tem teste automatizado e se confere abrindo o app com `mvn javafx:run`.

Verificação integrada com PostgreSQL real: veja [infra/verificacao](../../infra/verificacao/README.md).

Identidade visual: depois de editar `docs/marca/tokens.json`, rode `node docs/marca/gerar-tokens.mjs` na raiz. Ele atualiza o manual (`docs/marca/identidade-visual.html`), `admin-web/src/brand-tokens.css` e os `brand-tokens*.css` do PDV; confira o diff antes de commitar.

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
- Backend (`SaleLifecycleTests`): desconto do gerente, limite de desconto do operador, idempotência por `clientSaleId`, cancelamento com devolução de estoque, "minhas vendas" do operador e cadastro rápido de cliente.
- Backend (`OfflineStatusTests`): reenvio sem duplicar venda, venda offline com desconto, desconto acima do limite virando conflito e consulta de status após resolução.
- Backend (`AuditControllerTests`): auditoria de produto, consulta restrita a `ADMIN` e nota de entrada multi-item atômica.
- Backend (`PasswordAndOriginTests`): redefinição de senha pelo admin, troca da própria senha, admin impedido de se desativar ou rebaixar e recusa de escrita por cookie de origem não confiável.
- Backend (`SecurityErrorResponseTests`, servidor HTTP real em porta aleatória): usuário autenticado sem permissão recebe 403 `FORBIDDEN` (e não 401, que o admin web trata como sessão expirada) e requisição sem token recebe 401 `UNAUTHENTICATED`, ambos no envelope da API. O MockMvc não repete o despacho de erro do contêiner e não enxerga esse defeito.
- Backend (`ModuleBoundaryTests`, ArchUnit): módulos não dependem de `api` nem de `infrastructure` de outros módulos.
- Backend (`PostgresMigrationTests`, Testcontainers): migrations Flyway V1–V8 aplicadas em PostgreSQL real, validação das entidades e fluxo básico de venda. Ignorado sem Docker.
- Admin web: testes de raiz/shell, `AuthService` (cookie/`sessionStorage`, sem JWT em `localStorage`), `AccessService`, `InventoryService`, `CustomerService`, `SalesService`, `CatalogService`, `SupplierService`, `OfflineService`, interceptor com `withCredentials`, validação/erro do login, timeout de requisição sem resposta, dashboard, vendas e formulários/páginas de usuários, estoque, categorias, produtos, clientes, fornecedores e conflitos offline.
- PDV desktop: teste de `OfflineStorageConfig`, fila offline SQLite, catálogo local SQLite e cliente HTTP (login, consultas e corpo de `POST /api/sales` / sync).
- PDV desktop (`BarcodeScanTests`, `CartLinesTests`): regra do leitor de código de barras (código exato de barras ou SKU, ambiguidade, termo parcial e busca por nome nunca adicionam sozinhos) e soma de quantidades do mesmo produto no carrinho.
- Admin web (`ScanFieldDirective`, unitário e e2e): o Enter do leitor no campo "Código de barras" do cadastro de produto não envia o formulário e passa o foco para o SKU; o e2e falha sem a diretiva.
- PDV desktop (`CheckoutServiceTests`): confirmação online com `clientSaleId`, fila quando a API cai e reenvio, erro 5xx sem travar a fila, recusa de negócio como `REJECTED`, sessão expirada, venda sem sessão, validação de pagamentos múltiplos e desconto, reflexo da resolução de conflito e comprovante com desconto e aviso não fiscal.

O CI (`.github/workflows/ci.yml`) roda `mvn -B test` no backend e no PDV e `npm test` e `npm run build` no admin web, em pull requests e em push na `main`. Os testes e2e (Playwright) não rodam no CI.

## Estado de verificação

Última verificação completa: 23/11/2026, com JDK 25, na `main`.

| Camada | Resultado |
| --- | --- |
| Backend (`mvn test`) | 59 testes, 0 falhas, nenhum ignorado. Inclui `PostgresMigrationTests` (Testcontainers), com as migrations V1–V8 em PostgreSQL real |
| Admin web (`npm test`) | 75 specs. `npm run build` ok. `npm run test:e2e`: 12 cenários em Chromium desktop e celular |
| PDV (`mvn test`) | 29 testes, 0 falhas |
| Roteiro integrado ([infra/verificacao](../../infra/verificacao/README.md)) | 71 verificações contra a API real com PostgreSQL: permissões por perfil, cadastros, estoque, venda online, cancelamento, offline com conflito, relatórios, auditoria e senha |
| Admin web real × API real | As 10 telas carregam dados reais pelo navegador, com login por cookie, sem resposta de erro e sem erro no console |
| PDV real × API real | `CheckoutService` e fila SQLite reais: venda online, fila sem API, sincronização, conflito de preço e aceite no admin. O estoque do servidor fechou exatamente |
| Leitor de código de barras no PDV (simulado) | 15 verificações com eventos de teclado do JavaFX disparados dentro do PDV real e catálogo local temporário: adição sozinha com o foco fora da busca, soma de quantidades, leituras seguidas sem se misturar, código desconhecido, busca por nome, Enter solto sem acionar o login e campos de texto não afetados. Repetida 3 vezes |

A verificação integrada encontrou um defeito que nenhum teste anterior pegava: negação de permissão devolvia 401 em vez de 403 no servidor real. Corrigido em `SecurityErrorHandlers` e coberto por `SecurityErrorResponseTests`.

O CI roda os testes de backend, PDV e admin (unitários e build). O roteiro integrado, o e2e e a verificação de PDV e admin contra a API real são manuais.

## Diretrizes

- Para correções de bugs, adicionar teste de regressão quando for prático.
- Para nova fatia de backend, testar regra de domínio e contrato HTTP principal.
- Para nova tela Angular, testar renderização básica, integração com service e estados de erro relevantes.
- Para PDV/offline, testar preservação local da venda antes de testar sincronização.
- Não remover ou enfraquecer teste para obter build verde sem entender a causa.
