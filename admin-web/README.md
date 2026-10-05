# Admin Web

Administrativo web do Mercado One em Angular 22. Interface de administrador, gerente e estoquista. `OPERADOR_CAIXA` autentica, mas não tem rota no shell.

O que as telas fazem: [Uso do admin web](../docs/uso-admin-web.md). Inventário implementado: [Estado atual](../docs/estado-atual.md).

Credencial inicial de desenvolvimento: `admin` / `admin123`.

## Rotas atuais

```text
/login        Login do admin web
/             Início (ADMIN, GERENTE, ESTOQUISTA)
/usuarios     Usuários e perfis (ADMIN)
/produtos     Produtos (ADMIN, GERENTE)
/categorias   Categorias (ADMIN, GERENTE)
/estoque      Saldos, entradas, ajustes e movimentações (ADMIN, GERENTE, ESTOQUISTA)
/vendas       Relatório de vendas (ADMIN, GERENTE)
/offline      Conflitos offline (ADMIN, GERENTE)
/clientes     Clientes (ADMIN, GERENTE)
/fornecedores Fornecedores (ADMIN, GERENTE)
```

Sessão: cookie HttpOnly `mercado_one_admin_session` na API e metadados em `sessionStorage` (`mercado-one-admin-session-state`). O JWT não vai para `localStorage`. `src/environments/environment.ts` aponta para `http://localhost:8080`. O build publicado em outra origem usa `MERCADO_ONE_API_BASE_URL`.

## Comandos

```bash
npm install
npm start
npm test -- --watch=false
npm run test:e2e
npm run build
MERCADO_ONE_API_BASE_URL=https://exemplo.azurewebsites.net npm run build:production
```

`npm test` já inclui `--watch=false` e usa Karma `ChromeHeadlessNoGpu`. Para `npm run test:e2e`, mantenha `npm start` em outro terminal; os testes Playwright simulam as respostas da API. O build grava em `../output/admin-web-dist`. `build:production` grava a URL pública da API no bundle.

## Documentação relacionada

- [Contratos de API](../docs/api-contratos.md)
- [Segurança](../docs/seguranca.md)
- [Testes e verificação](../docs/testes-e-verificacao.md)
