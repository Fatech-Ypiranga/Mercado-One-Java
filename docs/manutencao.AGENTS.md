# Manutenção para Agentes

Este documento é voltado para agentes que vão editar e manter o Mercado One.

## Princípios

- Diferenciar sempre implementado, planejado e fora de escopo.
- Preferir pequenas fatias verificáveis.
- Manter nomes de domínio alinhados ao `CONTEXT.md`.
- Evitar criar abstrações antes de existir regra concreta.
- Atualizar documentação junto com mudanças de contrato, schema ou fluxo.
- Usar o código como fonte de verdade para estado atual; documentos de requisitos descrevem intenção e aceite, não necessariamente progresso.

## Backend

- Coloque regra de negócio no módulo dono em `modules/`.
- Use `api/common` apenas para contratos HTTP transversais.
- Use `infrastructure` raiz apenas para configurações e adaptadores transversais.
- Não exponha entidade JPA diretamente como DTO público.
- Mantenha `ddl-auto=validate`; schema deve evoluir via Flyway.
- Ao criar endpoints, testar o contrato principal com MockMvc ou teste equivalente.
- Se criar ou expandir auditoria, manter `audit` como módulo próprio e registrar o evento dentro da transação da operação auditada. Não há endpoint de consulta de `audit_events`.
- Redefinição administrativa de senha já existe: `PUT /api/access/users/{id}` com `password` opcional.
- Entrada de estoque é por produto (`POST /api/inventory/entries`), não um documento multi-item.
- `POST /api/sales` confirma venda com preço vigente do servidor. O PDV atual não é o consumidor desse endpoint.

## Admin web

- As rotas do shell já existem; ao criar outra, alinhar guard, item de navegação e `docs/uso-admin-web.md`.
- Usar `ApiClientService` ou services dedicados para contratos HTTP.
- Tipos TypeScript devem refletir o envelope da API.
- Telas administrativas devem tratar loading, vazio, erro e sucesso.
- Guards por perfil acompanham a autorização da API. `OPERADOR_CAIXA` só acessa `meu-caixa` e `minha-conta`.
- Sessão do admin: cookie HttpOnly `mercado_one_admin_session` + metadados em `sessionStorage`. Não gravar JWT em `localStorage`.
- Histórico de cliente é a tela `/vendas?customerId=`.

## PDV desktop

- O PDV deve preservar venda local antes de qualquer tentativa de sincronização. A UI atual sempre segue esse caminho, inclusive com API disponível.
- Falha ou conflito não deve apagar venda offline original.
- SQLite local é apoio offline, não fonte primária de cadastro.
- Fluxo de venda deve priorizar operação rápida de caixa.
- Catálogo local e fila offline devem continuar separados do servidor como fonte de verdade.
- Reenvio automático cobre vendas `PENDING` e `ERROR` no login. `CONFLICT` não é reenviado; a resolução fica no admin web e não altera o status local.
- `SyncStatus` é calculado por `CheckoutService.currentStatus()` a partir da fila local e da última resposta da API.

## Documentação

- README raiz deve ser mapa do projeto.
- `docs/README.md` deve continuar como índice dos guias.
- READMEs locais devem explicar responsabilidades e comandos do subprojeto.
- `docs/backlog-mvp.md` deve mudar no mesmo PR que entrega ou remove funcionalidade.
- `docs/backlog-mvp.md` deve mudar quando fatias forem entregues ou repriorizadas.
- `docs/diagramas.md` é a fonte normativa dos fluxos.
- ADRs em `docs/adr/` devem ser curtos e usados apenas para decisões relevantes.

## Antes de finalizar mudanças

- Revisar diff e links internos.
- Rodar checks relevantes se as ferramentas estiverem disponíveis.
- Declarar qualquer check não executado e o motivo.
