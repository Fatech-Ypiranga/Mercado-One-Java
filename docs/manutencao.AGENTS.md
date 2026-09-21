# Manutencao para Agentes

Este documento e voltado para agentes que vao editar e manter o Mercado One.

## Principios

- Diferenciar sempre implementado, planejado e fora de escopo.
- Preferir pequenas fatias verificaveis.
- Manter nomes de dominio alinhados ao `CONTEXT.md`.
- Evitar criar abstracoes antes de existir regra concreta.
- Atualizar documentacao junto com mudancas de contrato, schema ou fluxo.
- Usar o codigo como fonte de verdade para estado atual; documentos de requisitos descrevem intencao e aceite, nao necessariamente progresso.

## Backend

- Coloque regra de negocio no modulo dono em `modules/`.
- Use `api/common` apenas para contratos HTTP transversais.
- Use `infrastructure` raiz apenas para configuracoes e adaptadores transversais.
- Nao exponha entidade JPA diretamente como DTO publico.
- Mantenha `ddl-auto=validate`; schema deve evoluir via Flyway.
- Ao criar endpoints, testar o contrato principal com MockMvc ou teste equivalente.
- Se criar ou expandir auditoria, manter `audit` como modulo proprio e registrar o evento dentro da transacao da operacao auditada. Nao ha endpoint de consulta de `audit_events`.
- Redefinicao administrativa de senha ja existe: `PUT /api/access/users/{id}` com `password` opcional.
- Entrada de estoque e por produto (`POST /api/inventory/entries`), nao um documento multi-item.
- `POST /api/sales` confirma venda com preco vigente do servidor. O PDV atual nao e o consumidor desse endpoint.

## Admin web

- Manter shell e rotas simples ate as telas reais existirem.
- Usar `ApiClientService` ou services dedicados para contratos HTTP.
- Tipos TypeScript devem refletir o envelope da API.
- Telas administrativas devem tratar loading, vazio, erro e sucesso.
- Guards por perfil acompanham a autorizacao da API. `OPERADOR_CAIXA` nao tem rota no shell; o perfil e do PDV.
- Sessao do admin: cookie HttpOnly `mercado_one_admin_session` + metadados em `sessionStorage`. Nao gravar JWT em `localStorage`.
- Historico de cliente e a tela `/vendas?customerId=`.

## PDV desktop

- O PDV deve preservar venda local antes de qualquer tentativa de sincronizacao. A UI atual sempre segue esse caminho, inclusive com API disponivel.
- Falha ou conflito nao deve apagar venda offline original.
- SQLite local e apoio offline, nao fonte primaria de cadastro.
- Fluxo de venda deve priorizar operacao rapida de caixa.
- Catalogo local e fila offline devem continuar separados do servidor como fonte de verdade.
- Reenvio automatico cobre vendas `PENDING` e `ERROR` no login. `CONFLICT` nao e reenviado; a resolucao fica no admin web e nao altera o status local.
- O enum `SyncStatus` existe, mas o cabecalho da tela atual nao atualiza o badge apos o start; o operador ve feedback textual e status por venda na fila.

## Documentacao

- README raiz deve ser mapa do projeto.
- `docs/README.md` deve continuar como indice dos guias.
- READMEs locais devem explicar responsabilidades e comandos do subprojeto.
- `docs/estado-atual.md` deve mudar quando scaffold virar funcionalidade real.
- `docs/backlog-mvp.md` deve mudar quando fatias forem entregues ou repriorizadas.
- `docs/diagramas.md` e a fonte normativa dos fluxos; JSON/HTML em `docs/diagramas/` sao copias visuais e podem atrasar.
- ADRs em `docs/adr/` devem ser curtos e usados apenas para decisoes relevantes.

## Antes de finalizar mudancas

- Revisar diff e links internos.
- Rodar checks relevantes se as ferramentas estiverem disponiveis.
- Declarar qualquer check nao executado e o motivo.
