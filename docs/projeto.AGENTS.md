# Projeto para Agentes

Este documento é voltado para agentes. Use-o como mapa rápido antes de editar o Mercado One.

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

O inventário do que está implementado fica em `docs/estado-atual.md`. Não copie essa lista para outros documentos.

## Fatos fáceis de documentar errado

- A UI do PDV não chama `POST /api/sales`. Toda finalização grava em SQLite e depois tenta `POST /api/offline/sales/sync`.
- `SaleStatus` no backend só tem `CONFIRMED`. Não existe cancelamento pós-venda.
- Login seed de desenvolvimento é `admin` / `admin123`, não `admin@mercado.one`.
- `MERCADO_ONE_JWT_SECRET` é obrigatório para `mvn spring-boot:run`.
- `OPERADOR_CAIXA` é perfil do PDV; o admin web não tem telas para esse perfil.
- Conflito resolvido no admin não atualiza o status local da venda no PDV; o registro local permanece `CONFLICT`.

## Regras importantes

- Documentação específica para agentes deve usar sufixo `.AGENTS.md`.
- `CONTEXT.md` é apenas glossário de domínio; não colocar detalhes de implementação nele.
- README de subprojeto explica responsabilidade e comandos. O inventário funcional fica em `docs/estado-atual.md`.
- Ao alterar backend, respeitar fatias verticais por módulo.
- Ao alterar contratos HTTP, atualizar `docs/api-contratos.md` e o cliente Angular quando aplicável.
- Ao alterar schema servidor, criar migration Flyway e atualizar `docs/dados-e-migracoes.md`.
- Ao alterar offline do PDV ou resolução de conflitos, atualizar `docs/offline-pdv-sync.md` e `docs/api-contratos.md`.
- Ao alterar arquitetura, módulos ou fluxos críticos, atualizar `docs/diagramas.md`.
- Não inventar funcionalidade na documentação. Se o código não tem a fatia, registre como não implementado em `docs/estado-atual.md` ou `docs/backlog-mvp.md`.

## Verificação

Checks esperados:

```bash
cd backend-api && mvn test
cd admin-web && npm test -- --watch=false
cd pdv-desktop && mvn test
```

Não afirmar que passaram sem executar. Se ferramenta local estiver ausente, registrar a limitação.
