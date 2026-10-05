# Documentação do Mercado One

Índice dos guias. O mapa de comandos e pastas é o `README.md` da raiz. O glossário de domínio é o `CONTEXT.md`. O inventário do que o código faz hoje é [estado-atual.md](estado-atual.md).

## Entrada

| Documento | Uso |
| --- | --- |
| [../README.md](../README.md) | Mapa do monorepo, stack e comandos |
| [../CONTEXT.md](../CONTEXT.md) | Glossário de domínio |
| [../AGENTS.md](../AGENTS.md) | Ponte para docs de agentes |
| [estado-atual.md](estado-atual.md) | O que existe no código agora |
| [arquitetura.md](arquitetura.md) | Componentes, fronteiras e fluxo de venda |
| [backlog-mvp.md](backlog-mvp.md) | Trabalho restante |

## Uso da loja

| Documento | Uso |
| --- | --- |
| [uso-admin-web.md](uso-admin-web.md) | Tarefas do administrador, gerente e estoquista |
| [uso-pdv.md](uso-pdv.md) | Jornada do operador de caixa |

## Operação e contratos

| Documento | Uso |
| --- | --- |
| [desenvolvimento-local.md](desenvolvimento-local.md) | Setup, variáveis, portas e troubleshooting |
| [testes-e-verificacao.md](testes-e-verificacao.md) | Checks e suíte conhecida |
| [api-contratos.md](api-contratos.md) | Envelope, endpoints, erros e autorização |
| [dados-e-migracoes.md](dados-e-migracoes.md) | PostgreSQL, Flyway e SQLite do PDV |
| [modelo-conceitual.md](modelo-conceitual.md) | MER conceitual com entidades, relacionamentos, atributos e cardinalidades |
| [dicionario-de-dados.md](dicionario-de-dados.md) | Dicionário físico dos campos persistidos |
| [seguranca.md](seguranca.md) | JWT, cookie, CORS, perfis e auditoria |
| [offline-pdv-sync.md](offline-pdv-sync.md) | Fila local, sync e conflitos |
| [diagramas.md](diagramas.md) | Mermaid de arquitetura, domínio e fluxos |

## Intenção e decisões

| Documento | Uso |
| --- | --- |
| [requisitos-mvp-mercado-one.md](requisitos-mvp-mercado-one.md) | Requisitos e aceite; descreve intenção, não progresso |
| [adr/0001-monorepo-com-tres-aplicacoes.md](adr/0001-monorepo-com-tres-aplicacoes.md) | ADR: monorepo |
| [adr/0002-backend-em-fatias-verticais.md](adr/0002-backend-em-fatias-verticais.md) | ADR: fatias verticais |
| [adr/0003-pdv-desktop-javafx-com-sqlite-local.md](adr/0003-pdv-desktop-javafx-com-sqlite-local.md) | ADR: PDV JavaFX + SQLite |
| [adr/0004-postgresql-e-flyway-no-servidor.md](adr/0004-postgresql-e-flyway-no-servidor.md) | ADR: PostgreSQL + Flyway |

## Agentes

| Documento | Uso |
| --- | --- |
| [projeto.AGENTS.md](projeto.AGENTS.md) | Mapa rápido antes de editar |
| [manutencao.AGENTS.md](manutencao.AGENTS.md) | Convenções de manutenção por área |

## Subprojetos

| Documento | Uso |
| --- | --- |
| [../backend-api/README.md](../backend-api/README.md) | Comandos da API |
| [../admin-web/README.md](../admin-web/README.md) | Rotas, sessão e comandos Angular |
| [../pdv-desktop/README.md](../pdv-desktop/README.md) | Comandos do PDV JavaFX |
| [../infra/README.md](../infra/README.md) | PostgreSQL local via Compose e piloto Azure |
