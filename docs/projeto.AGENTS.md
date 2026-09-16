# Projeto para Agentes

Este documento e voltado para agentes. Use-o como mapa rapido antes de editar o Mercado One.

## Leitura inicial

Antes de editar, leia nesta ordem:

1. `README.md`
2. `CONTEXT.md`
3. `docs/estado-atual.md`
4. `docs/arquitetura.md`
5. `docs/backlog-mvp.md`
6. `docs/diagramas.md`

Depois leia o README do subprojeto afetado:

- `backend-api/README.md`
- `admin-web/README.md`
- `pdv-desktop/README.md`
- `infra/README.md`

## Estado do projeto

O Mercado One ja possui fatias funcionais do MVP. Diferencie o que esta implementado do que ainda e refinamento operacional.

Implementado agora:

- API com contratos basicos, autenticacao, perfis, usuarios, catalogo, clientes, fornecedores, estoque, vendas, relatorio de vendas, produtos mais vendidos, sync offline, conflitos offline resoluveis e auditoria inicial.
- Admin com login, shell protegido e telas funcionais de usuarios, categorias, produtos, estoque, vendas, conflitos offline, clientes e fornecedores.
- PDV com login de operador, busca online com fallback de catalogo local SQLite, carrinho, comprovante simples nao fiscal, fila offline local, estados `PENDING`, `SENT`, `CONFLICT` e `ERROR`, e sincronizacao.
- Infra local com PostgreSQL.

Planejado:

- Evolucoes de UX, padronizacao de mensagens, relatorios adicionais e refinamentos operacionais para piloto.

## Regras importantes

- Documentacao especifica para agentes deve usar sufixo `.AGENTS.md`.
- Documentacao de dominio pode ficar no README local ou em `docs/` do subprojeto.
- `CONTEXT.md` e apenas glossario de dominio; nao colocar detalhes de implementacao nele.
- Ao alterar backend, respeitar fatias verticais por modulo.
- Ao alterar contratos HTTP, atualizar `docs/api-contratos.md` e o cliente Angular quando aplicavel.
- Ao alterar schema servidor, criar migration Flyway e atualizar docs de dados.
- Ao alterar offline do PDV ou resolucao de conflitos, atualizar `docs/offline-pdv-sync.md`, `pdv-desktop/docs/offline-sync.md` e `docs/api-contratos.md`.
- Ao alterar arquitetura, modulos ou fluxos criticos, atualizar `docs/diagramas.md` e, quando aplicavel, os JSON em `docs/diagramas/`.

## Verificacao

Checks esperados:

```bash
cd backend-api && mvn test
cd admin-web && npm test -- --watch=false
cd pdv-desktop && mvn test
```

Nao afirmar que passaram sem executar. Se ferramenta local estiver ausente, registrar a limitacao.
