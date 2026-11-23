# Admin Web

Administrativo web do Mercado One em Angular 22. Interface de administrador, gerente e estoquista. `OPERADOR_CAIXA` acessa apenas "Meu caixa" e "Minha conta".

O que as telas fazem: [Uso do admin web](../docs/produto/uso-admin-web.md). Entregue e pendente: [Backlog MVP](../docs/produto/backlog-mvp.md).

Credencial inicial de desenvolvimento: `admin` / `admin123`.

## Rotas atuais

```text
/login        Login do admin web
/esqueci-senha Pede o link de redefinição por e-mail, ou orienta falar com o administrador se o servidor não envia e-mail (público)
/nova-senha    Cria a senha nova a partir do link do e-mail, com tela de link expirado (público)
/suporte      Canais de suporte configurados no ambiente (público)
/             Início (ADMIN, GERENTE, ESTOQUISTA)
/usuarios     Usuários e perfis (ADMIN)
/produtos     Produtos (ADMIN, GERENTE)
/categorias   Categorias (ADMIN, GERENTE)
/estoque      Saldos, entradas, ajustes e movimentações (ADMIN, GERENTE, ESTOQUISTA)
/vendas       Relatório de vendas (ADMIN, GERENTE)
/offline      Conflitos offline (ADMIN, GERENTE)
/clientes     Clientes (ADMIN, GERENTE)
/fornecedores Fornecedores (ADMIN, GERENTE)
/auditoria    Auditoria (ADMIN)
/meu-caixa    Vendas do próprio usuário no dia (ADMIN, GERENTE, OPERADOR_CAIXA)
/minha-conta  Troca da própria senha (todos os perfis)
```

`OPERADOR_CAIXA` entra em `/meu-caixa`; os demais perfis entram em `/`.

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

Telas de acesso (`/login`, `/esqueci-senha`, `/suporte`): `src/app/features/account/auth-shell.component.*` traz a moldura e o estilo compartilhados (fundo, marca, card), com as cores dos tokens da marca. A tela de esqueci a senha pergunta ao servidor se há envio de e-mail configurado (`GET /api/auth/password-reset/status`): com e-mail, mostra o formulário; sem, explica a redefinição pelo administrador em vez de simular um envio. `/nova-senha` lê o token do link, tira-o da barra de endereços e mostra "link expirado" quando ele não vale mais.

`npm test` já inclui `--watch=false` e usa Karma `ChromeHeadlessNoGpu`. Para `npm run test:e2e`, mantenha `npm start` em outro terminal; os testes Playwright simulam as respostas da API. O build grava em `../output/admin-web-dist`. `build:production` grava a URL pública da API no bundle e, se definidas, os canais da tela de suporte: `MERCADO_ONE_SUPPORT_WHATSAPP` (só dígitos, com 55 e DDD, ex.: `5517999999999`), `MERCADO_ONE_SUPPORT_EMAIL` e `MERCADO_ONE_SUPPORT_HOURS` (texto livre). Todas são opcionais; o que ficar vazio não aparece. O script recusa WhatsApp ou e-mail em formato inválido. Em desenvolvimento os valores ficam em `src/environments/environment.ts`, com um WhatsApp de teste que deve ser trocado.

## Visual

Cores, fontes e medidas seguem a [identidade visual](../docs/marca/identidade-visual.html). `src/brand-tokens.css` é gerado de `docs/marca/tokens.json` por `node docs/marca/gerar-tokens.mjs` (na raiz) e não deve ser editado à mão. `src/styles.css` só liga as variáveis dos componentes (`--color-*`) a esses tokens. O modo claro/escuro fica em `ThemeService` (`data-theme` no `<html>`, escolha salva em `localStorage` como `mo-theme`).

## Documentação relacionada

- [Contratos de API](../docs/desenvolvimento/api-contratos.md)
- [Segurança](../docs/arquitetura/seguranca.md)
- [Testes e verificação](../docs/desenvolvimento/testes-e-verificacao.md)
