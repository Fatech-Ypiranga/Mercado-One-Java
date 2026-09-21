# Documentacao do Mercado One

Ponto de indice dos guias do repositorio. O mapa do projeto continua sendo o `README.md` da raiz; o glossario de dominio e o `CONTEXT.md`.

## Entrada

| Documento | Uso |
| --- | --- |
| [../README.md](../README.md) | Mapa do monorepo, stack, comandos e estado resumido |
| [../CONTEXT.md](../CONTEXT.md) | Glossario de dominio; sem detalhes de implementacao |
| [../AGENTS.md](../AGENTS.md) | Ponte para docs de agentes |
| [estado-atual.md](estado-atual.md) | O que existe no codigo agora |
| [arquitetura.md](arquitetura.md) | Componentes, fronteiras e dados |
| [backlog-mvp.md](backlog-mvp.md) | Fatias entregues e trabalho restante |

## Operacao e contratos

| Documento | Uso |
| --- | --- |
| [desenvolvimento-local.md](desenvolvimento-local.md) | Setup, variaveis, portas e troubleshooting |
| [testes-e-verificacao.md](testes-e-verificacao.md) | Checks e suite conhecida |
| [api-contratos.md](api-contratos.md) | Envelope, endpoints, erros e autorizacao |
| [dados-e-migracoes.md](dados-e-migracoes.md) | PostgreSQL, Flyway e SQLite do PDV |
| [seguranca.md](seguranca.md) | JWT, cookie, CORS, perfis e auditoria |
| [offline-pdv-sync.md](offline-pdv-sync.md) | Fila local, sync e conflitos |

## Intencao e decisoes

| Documento | Uso |
| --- | --- |
| [requisitos-mvp-mercado-one.md](requisitos-mvp-mercado-one.md) | Requisitos e aceite; descreve intencao, nao progresso |
| [decisoes-stack-scaffold.md](decisoes-stack-scaffold.md) | Stack travada e regras do scaffold |
| [adr/0001-monorepo-com-tres-aplicacoes.md](adr/0001-monorepo-com-tres-aplicacoes.md) | ADR: monorepo |
| [adr/0002-backend-em-fatias-verticais.md](adr/0002-backend-em-fatias-verticais.md) | ADR: fatias verticais |
| [adr/0003-pdv-desktop-javafx-com-sqlite-local.md](adr/0003-pdv-desktop-javafx-com-sqlite-local.md) | ADR: PDV JavaFX + SQLite |
| [adr/0004-postgresql-e-flyway-no-servidor.md](adr/0004-postgresql-e-flyway-no-servidor.md) | ADR: PostgreSQL + Flyway |

## Diagramas

| Documento | Uso |
| --- | --- |
| [diagramas.md](diagramas.md) | Mermaid normativo de arquitetura, dominio e fluxos |
| [diagramas/README.md](diagramas/README.md) | Copias visuais JSON/HTML; podem estar defasadas |
| [diagramas-interface-web/README.md](diagramas-interface-web/README.md) | Wireframes e mapas da interface admin (planejamento) |

`UML_drawio(1).xml` e um sketch conceitual de estoque no draw.io. Nao e fonte de verdade; use `diagramas.md` e o codigo.

## Agentes

| Documento | Uso |
| --- | --- |
| [projeto.AGENTS.md](projeto.AGENTS.md) | Mapa rapido antes de editar |
| [manutencao.AGENTS.md](manutencao.AGENTS.md) | Convenoes de manutencao por area |

## Subprojetos

| Documento | Uso |
| --- | --- |
| [../backend-api/README.md](../backend-api/README.md) | API, modulos e comandos |
| [../backend-api/docs/modulos.md](../backend-api/docs/modulos.md) | Fatias do backend |
| [../backend-api/docs/persistencia.md](../backend-api/docs/persistencia.md) | Flyway e `application.yml` |
| [../admin-web/README.md](../admin-web/README.md) | Rotas, sessao e comandos Angular |
| [../pdv-desktop/README.md](../pdv-desktop/README.md) | PDV JavaFX e comandos |
| [../pdv-desktop/docs/offline-sync.md](../pdv-desktop/docs/offline-sync.md) | SQLite local e fila do PDV |
| [../infra/README.md](../infra/README.md) | PostgreSQL local via Compose |
