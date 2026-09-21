# Projeto para Agentes

Este documento e voltado para agentes. Use-o como mapa rapido antes de editar o Mercado One.

## Leitura inicial

Antes de editar, leia nesta ordem:

1. `README.md`
2. `CONTEXT.md`
3. `docs/README.md`
4. `docs/estado-atual.md`
5. `docs/arquitetura.md`
6. `docs/backlog-mvp.md`
7. `docs/diagramas.md`

Depois leia o README do subprojeto afetado:

- `backend-api/README.md`
- `admin-web/README.md`
- `pdv-desktop/README.md`
- `infra/README.md`

## Estado do projeto

O Mercado One ja possui fatias funcionais do MVP. Diferencie o que esta implementado do que ainda e refinamento operacional. Use o codigo como fonte de verdade.

Implementado agora:

- API com contratos basicos, autenticacao, perfis, usuarios, catalogo, clientes, fornecedores, estoque, venda confirmada no servidor (`POST /api/sales`), relatorio de vendas, produtos mais vendidos, sync offline, conflitos offline resoluveis e auditoria inicial (apenas gravacao).
- Admin com login, shell protegido e telas funcionais de usuarios, categorias, produtos, estoque, vendas, conflitos offline, clientes e fornecedores.
- PDV com login de operador, busca online com fallback de catalogo local SQLite, carrinho, comprovante simples nao fiscal, fila offline local, estados `PENDING`, `SENT`, `CONFLICT` e `ERROR`, e sincronizacao via `POST /api/offline/sales/sync`.
- Infra local com PostgreSQL.

Fatos faceis de documentar errado:

- A UI do PDV nao chama `POST /api/sales`. Toda finalizacao grava em SQLite e depois tenta `POST /api/offline/sales/sync`.
- `SaleStatus` no backend so tem `CONFIRMED`. Nao existe cancelamento pos-venda.
- Login seed de desenvolvimento e `admin` / `admin123`, nao `admin@mercado.one`.
- `MERCADO_ONE_JWT_SECRET` e obrigatorio para `mvn spring-boot:run`.
- `OPERADOR_CAIXA` e perfil do PDV; o admin web nao tem telas para esse perfil.
- Conflito resolvido no admin nao atualiza o status local da venda no PDV; o registro local permanece `CONFLICT`.

Planejado:

- Evolucoes de UX, padronizacao de mensagens, consulta de auditoria, desconto simples (requisito conceitual) e refinamentos operacionais para piloto.

## Regras importantes

- Documentacao especifica para agentes deve usar sufixo `.AGENTS.md`.
- Documentacao de dominio pode ficar no README local ou em `docs/` do subprojeto.
- `CONTEXT.md` e apenas glossario de dominio; nao colocar detalhes de implementacao nele.
- Ao alterar backend, respeitar fatias verticais por modulo.
- Ao alterar contratos HTTP, atualizar `docs/api-contratos.md` e o cliente Angular quando aplicavel.
- Ao alterar schema servidor, criar migration Flyway e atualizar docs de dados.
- Ao alterar offline do PDV ou resolucao de conflitos, atualizar `docs/offline-pdv-sync.md`, `pdv-desktop/docs/offline-sync.md` e `docs/api-contratos.md`.
- Ao alterar arquitetura, modulos ou fluxos criticos, atualizar `docs/diagramas.md` e, quando aplicavel, os JSON em `docs/diagramas/`.
- Nao inventar funcionalidade na documentacao. Se o codigo nao tem a fatia, registre como nao implementado.

## Verificacao

Checks esperados:

```bash
cd backend-api && mvn test
cd admin-web && npm test -- --watch=false
cd pdv-desktop && mvn test
```

Nao afirmar que passaram sem executar. Se ferramenta local estiver ausente, registrar a limitacao.
