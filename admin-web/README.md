# Admin Web

Administrativo web do Mercado One em Angular 22. Interface de administrador, gerente e estoquista. `OPERADOR_CAIXA` autentica, mas nao tem rota no shell.

O que as telas fazem: [Uso do admin web](../docs/uso-admin-web.md). Inventario implementado: [Estado atual](../docs/estado-atual.md).

Credencial inicial de desenvolvimento: `admin` / `admin123`.

## Rotas atuais

```text
/login        Login do admin web
/             Inicio (ADMIN, GERENTE, ESTOQUISTA)
/usuarios     Usuarios e perfis (ADMIN)
/produtos     Produtos (ADMIN, GERENTE)
/categorias   Categorias (ADMIN, GERENTE)
/estoque      Saldos, entradas, ajustes e movimentacoes (ADMIN, GERENTE, ESTOQUISTA)
/vendas       Relatorio de vendas (ADMIN, GERENTE)
/offline      Conflitos offline (ADMIN, GERENTE)
/clientes     Clientes (ADMIN, GERENTE)
/fornecedores Fornecedores (ADMIN, GERENTE)
```

Sessao: cookie HttpOnly `mercado_one_admin_session` na API e metadados em `sessionStorage` (`mercado-one-admin-session-state`). O JWT nao vai para `localStorage`. `src/environments/environment.ts` aponta para `http://localhost:8080`.

## Comandos

```bash
npm install
npm start
npm test -- --watch=false
npm run build
```

`npm test` ja inclui `--watch=false` e usa Karma `ChromeHeadlessNoGpu`. O build grava em `../output/admin-web-dist`.

## Documentacao relacionada

- [Contratos de API](../docs/api-contratos.md)
- [Seguranca](../docs/seguranca.md)
- [Testes e verificacao](../docs/testes-e-verificacao.md)
