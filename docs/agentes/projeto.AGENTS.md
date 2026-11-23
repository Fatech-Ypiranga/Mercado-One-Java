# Projeto para Agentes

Este documento é voltado para agentes. Use-o como mapa rápido antes de editar o Mercado One.

## Leitura inicial

Antes de editar, leia nesta ordem:

1. `README.md`
2. `CONTEXT.md`
3. `docs/README.md`
4. `docs/backlog-mvp.md`
5. `docs/arquitetura.md`
6. `docs/diagramas.md`

Depois leia o README do subprojeto afetado:

- `backend-api/README.md`
- `admin-web/README.md`
- `pdv-desktop/README.md`
- `infra/README.md`

O que está entregue e pendente fica em `docs/backlog-mvp.md`. Não copie essa lista para outros documentos.

## Fatos fáceis de documentar errado

- O PDV grava toda venda em SQLite antes da rede, confirma online por `POST /api/sales` e usa `POST /api/offline/sales/sync` para reenvio.
- `SaleStatus` tem `CONFIRMED` e `CANCELED`; cancelamento (`POST /api/sales/{id}/cancel`) devolve o estoque.
- Login seed de desenvolvimento é `admin` / `admin123`, não `admin@mercado.one`.
- `MERCADO_ONE_JWT_SECRET` é obrigatório para `mvn spring-boot:run`.
- `OPERADOR_CAIXA` usa o PDV; no admin web acessa apenas "Meu caixa" e "Minha conta".
- Conflito resolvido no admin é refletido no PDV pela consulta `GET /api/offline/sales/status` (`SENT` ou `REJECTED`).

## Regras importantes

- Documentação específica para agentes deve usar sufixo `.AGENTS.md`.
- `CONTEXT.md` é apenas glossário de domínio; não colocar detalhes de implementação nele.
- README de subprojeto explica responsabilidade e comandos. O que está entregue fica em `docs/backlog-mvp.md`.
- Ao alterar backend, respeitar fatias verticais por módulo.
- Ao alterar contratos HTTP, atualizar `docs/api-contratos.md` e o cliente Angular quando aplicável.
- Ao alterar schema servidor, criar migration Flyway e atualizar `docs/dados-e-migracoes.md`.
- Ao alterar offline do PDV ou resolução de conflitos, atualizar `docs/offline-pdv-sync.md` e `docs/api-contratos.md`.
- Ao alterar arquitetura, módulos ou fluxos críticos, atualizar `docs/diagramas.md`.
- Não inventar funcionalidade na documentação. Se o código não tem a fatia, registre como pendente em `docs/backlog-mvp.md`.

## Verificação

Checks esperados:

```bash
cd backend-api && mvn test
cd admin-web && npm test -- --watch=false
cd pdv-desktop && mvn test
```

Não afirmar que passaram sem executar. Se ferramenta local estiver ausente, registrar a limitação.
