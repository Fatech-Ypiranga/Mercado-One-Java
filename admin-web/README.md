# Admin Web

Administrativo web do Mercado One em Angular 22. Este subprojeto concentra a interface administrativa usada por administrador, gerente e estoquista autorizado. `OPERADOR_CAIXA` autentica, mas nao tem rota no shell.

## Estado atual

Implementado:

- Aplicacao Angular standalone, prefixo `mo`, `npm start` em `0.0.0.0:4200`.
- Login com `POST /api/auth/login` e `withCredentials`.
- Sessao: cookie HttpOnly na API; metadados em `sessionStorage` (`mercado-one-admin-session-state`). JWT nao vai para `localStorage`.
- Shell lateral com navegacao por departamento, filtrada por perfil.
- Guards por perfil alinhados a autorizacao da API.
- Dashboard com briefing do dia (conflitos e totais de venda para `ADMIN`/`GERENTE`), versao da API e atalhos.
- Telas funcionais de usuarios, categorias, produtos, estoque, vendas, conflitos offline, clientes e fornecedores.
- Historico de cliente via `/vendas?customerId=`.
- Identidade visual de balcao: papel de talao, Petrona e IBM Plex.
- Services: `ApiClientService`, `AuthService`, `AccessService`, `CatalogService`, `InventoryService`, `SalesService`, `CustomerService`, `SupplierService` e `OfflineService`.
- `src/environments/environment.ts` aponta para `http://localhost:8080`. `environment.prod.ts` usa `apiBaseUrl` vazio.

Credencial inicial de desenvolvimento: `admin` / `admin123`.

Ainda planejado:

- Tratamento padronizado completo de erros de API em todas as telas.
- O tipo `SaleStatus` em `sales.service.ts` declara `CANCELED`, valor inexistente no backend.

## Rotas atuais

```text
/login        Login do admin web
/             Dashboard (ADMIN, GERENTE, ESTOQUISTA)
/usuarios     Gestao de usuarios e perfis (ADMIN)
/produtos     Cadastro e consulta de produtos (ADMIN, GERENTE)
/categorias   Cadastro e consulta de categorias (ADMIN, GERENTE)
/estoque      Saldos, entradas, ajustes e movimentacoes (ADMIN, GERENTE, ESTOQUISTA)
/vendas       Relatorio de vendas e historico por cliente (ADMIN, GERENTE)
/offline      Resolucao administrativa de conflitos offline (ADMIN, GERENTE)
/clientes     Cadastro, consulta e status de clientes (ADMIN, GERENTE)
/fornecedores Cadastro, consulta e status de fornecedores (ADMIN, GERENTE)
```

## Comandos

```bash
npm install
npm start
npm test -- --watch=false
npm run build
```

`npm test` ja inclui `--watch=false` e usa Karma `ChromeHeadlessNoGpu`. O build grava em `../output/admin-web-dist`.

## Documentacao relacionada

- [Arquitetura](../docs/arquitetura.md)
- [Backlog MVP](../docs/backlog-mvp.md)
- [Contratos de API](../docs/api-contratos.md)
- [Testes e verificacao](../docs/testes-e-verificacao.md)
