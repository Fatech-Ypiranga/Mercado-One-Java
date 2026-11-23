# Documentação do Mercado One

Índice dos guias, organizados por assunto. O mapa de comandos e pastas é o `README.md` da raiz. O glossário de domínio é o `CONTEXT.md`. Entregue e pendente: [produto/backlog-mvp.md](produto/backlog-mvp.md).

## Estrutura

```text
docs/
  produto/          Requisitos, backlog e guias de uso (admin web e PDV)
  arquitetura/      Arquitetura, diagramas, segurança, offline/sync e ADRs
  desenvolvimento/  Setup local, testes e contratos de API
  dados/            Modelos conceitual, lógico e físico, dicionário e migrações
  marca/            Identidade visual, logo e tokens de design
  agentes/          Documentação voltada a agentes (sufixo .AGENTS.md)
  academico/        Trabalho do PI II
```

## Entrada

| Documento | Uso |
| --- | --- |
| [../README.md](../README.md) | Mapa do monorepo, stack e comandos |
| [../CONTEXT.md](../CONTEXT.md) | Glossário de domínio |
| [../AGENTS.md](../AGENTS.md) | Ponte para docs de agentes |

## Produto

| Documento | Uso |
| --- | --- |
| [produto/requisitos-mvp-mercado-one.md](produto/requisitos-mvp-mercado-one.md) | Requisitos e aceite; descreve intenção, não progresso |
| [produto/backlog-mvp.md](produto/backlog-mvp.md) | Entregue e pendente |
| [produto/uso-admin-web.md](produto/uso-admin-web.md) | Tarefas do administrador, gerente e estoquista |
| [produto/uso-pdv.md](produto/uso-pdv.md) | Jornada do operador de caixa |

## Arquitetura

| Documento | Uso |
| --- | --- |
| [arquitetura/arquitetura.md](arquitetura/arquitetura.md) | Componentes, fronteiras e fluxo de venda |
| [arquitetura/diagramas.md](arquitetura/diagramas.md) | Mermaid de arquitetura, domínio e fluxos |
| [arquitetura/seguranca.md](arquitetura/seguranca.md) | JWT, cookie, CORS, perfis e auditoria |
| [arquitetura/offline-pdv-sync.md](arquitetura/offline-pdv-sync.md) | Fila local, sync e conflitos |
| [arquitetura/adr/0001-monorepo-com-tres-aplicacoes.md](arquitetura/adr/0001-monorepo-com-tres-aplicacoes.md) | ADR: monorepo |
| [arquitetura/adr/0002-backend-em-fatias-verticais.md](arquitetura/adr/0002-backend-em-fatias-verticais.md) | ADR: fatias verticais |
| [arquitetura/adr/0003-pdv-desktop-javafx-com-sqlite-local.md](arquitetura/adr/0003-pdv-desktop-javafx-com-sqlite-local.md) | ADR: PDV JavaFX + SQLite |
| [arquitetura/adr/0004-postgresql-e-flyway-no-servidor.md](arquitetura/adr/0004-postgresql-e-flyway-no-servidor.md) | ADR: PostgreSQL + Flyway |

## Desenvolvimento

| Documento | Uso |
| --- | --- |
| [desenvolvimento/desenvolvimento-local.md](desenvolvimento/desenvolvimento-local.md) | Setup, variáveis, portas e troubleshooting |
| [desenvolvimento/testes-e-verificacao.md](desenvolvimento/testes-e-verificacao.md) | Checks e suíte conhecida |
| [desenvolvimento/api-contratos.md](desenvolvimento/api-contratos.md) | Envelope, endpoints, erros e autorização |
| [desenvolvimento/deploy-render.md](desenvolvimento/deploy-render.md) | Publicação da API e do painel no Render |

## Dados

| Documento | Uso |
| --- | --- |
| [dados/dados-e-migracoes.md](dados/dados-e-migracoes.md) | PostgreSQL, Flyway e SQLite do PDV |
| [dados/modelo-conceitual.md](dados/modelo-conceitual.md) | MER conceitual com entidades, relacionamentos, atributos e cardinalidades (diagramas `.puml`/`.svg` na mesma pasta) |
| [dados/modelo-logico-relacional.md](dados/modelo-logico-relacional.md) | MLR derivado do schema real, cardinalidades e normalização |
| [dados/modelo-fisico.sql](dados/modelo-fisico.sql) | Modelo físico: DDL do PostgreSQL gerado das migrations V1–V8 |
| [dados/dicionario-de-dados.md](dados/dicionario-de-dados.md) | Dicionário físico dos campos persistidos |

## Marca

| Documento | Uso |
| --- | --- |
| [marca/README.md](marca/README.md) | Estrutura da pasta, como mudar cor ou logo e qual logo usar |
| [marca/identidade-visual.html](marca/identidade-visual.html) | Logo, cores, tipografia, componentes e tokens para Angular e JavaFX (abra no navegador) |
| [marca/logo/](marca/logo/svg/mercadoone-horizontal.svg) | Logos em SVG e PNG, e os JPGs originais |
| [marca/tokens.json](marca/tokens.json) | Fonte única dos tokens; `node docs/marca/gerar-tokens.mjs` regenera o HTML, o CSS dos apps e copia os logos |

## Agentes

| Documento | Uso |
| --- | --- |
| [agentes/projeto.AGENTS.md](agentes/projeto.AGENTS.md) | Mapa rápido antes de editar |
| [agentes/manutencao.AGENTS.md](agentes/manutencao.AGENTS.md) | Convenções de manutenção por área |

## Acadêmico

| Documento | Uso |
| --- | --- |
| [academico/TRABALHO-PI-II-Mercado-One.docx](academico/TRABALHO-PI-II-Mercado-One.docx) | Documento do PI II |

## Subprojetos

| Documento | Uso |
| --- | --- |
| [../backend-api/README.md](../backend-api/README.md) | Comandos da API |
| [../admin-web/README.md](../admin-web/README.md) | Rotas, sessão e comandos Angular |
| [../pdv-desktop/README.md](../pdv-desktop/README.md) | Comandos do PDV JavaFX |
| [../infra/README.md](../infra/README.md) | PostgreSQL local via Compose e piloto Azure |
