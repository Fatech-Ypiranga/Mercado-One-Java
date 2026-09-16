# Mercado One

Mercado One e um ERP em scaffold funcional para pequenos mercados varejistas, com administrativo web, API Java e PDV desktop offline-first. O objetivo do repositorio e evoluir o MVP de loja fisica: cadastros, estoque simples, vendas presenciais, operacao offline parcial, clientes e relatorios operacionais.

## Estado atual

Este repositorio esta em fase de scaffold funcional. A stack, a estrutura de modulos e os contratos iniciais ja estao definidos, e as principais fatias operacionais do MVP ja existem em codigo. O backlog agora diferencia funcionalidades entregues de refinamentos para piloto.

Implementado agora:

- Backend Spring Boot com envelope HTTP padronizado, handler global de erro, autenticacao JWT/cookie HttpOnly, seed de administrador, autorizacao por perfil, gestao de usuarios/perfis, catalogo de categorias/produtos, clientes, fornecedores, estoque simples, venda online, relatorio de vendas paginado/CSV, produtos mais vendidos, sync offline, conflitos offline resoluveis, auditoria inicial e `GET /api/system/info`.
- Admin Angular com login, shell protegido, guards por perfil, dashboard inicial e telas funcionais de usuarios, categorias, produtos, estoque, vendas, conflitos offline, clientes e fornecedores.
- PDV JavaFX com login de operador, busca online de produtos/clientes, fallback de catalogo local SQLite, carrinho, total, comprovante simples nao fiscal, fila SQLite local e sincronizacao de venda offline via API.
- Infra local com PostgreSQL 18 via Docker Compose.
- Testes minimos nos tres subprojetos.

Planejado para o MVP:

- Refinamentos de UX para operacao piloto.
- Padronizacao completa de mensagens e tratamento visual de erro em todas as telas.
- Relatorios adicionais alem dos operacionais ja implementados.
- Dados fiscais apenas preparatorios, sem emissao fiscal no MVP.

## Stack

- Backend: Java 25 LTS, Spring Boot 4.1.x, Maven, PostgreSQL 18, Flyway, JPA, Validation, Security e Actuator.
- Admin web: Angular 22, TypeScript, HTML e CSS.
- PDV desktop: Java 25 LTS, JavaFX e SQLite local.
- Infra local: Docker Compose para PostgreSQL.

## Estrutura

```text
backend-api/   API REST e dominio servidor
admin-web/     Administrativo web Angular
pdv-desktop/   Cliente desktop JavaFX para PDV
infra/         Infraestrutura local de desenvolvimento
docs/          Requisitos, arquitetura, backlog e guias transversais
CONTEXT.md     Glossario de dominio
```

## Arquitetura

O backend usa fatias verticais por capacidade de negocio. Cada modulo deve concentrar sua propria interface HTTP, casos de uso, dominio e adaptadores internos quando a fatia for implementada.

```text
backend-api/src/main/java/com/mercadoone/backend/modules/
  access/
  catalog/
  customer/
  inventory/
  sales/
  offline/
  supplier/
  audit/
  system/
```

Contratos HTTP compartilhados ficam em `api/common`; configuracoes transversais ficam em `infrastructure`. Modulos nao devem depender de detalhes internos de outros modulos.

## Desenvolvimento local

1. Configure variaveis a partir de `.env.example`.
2. Suba o banco:

```bash
docker compose -f infra/docker-compose.yml up -d
```

3. Execute a API:

```bash
cd backend-api
mvn spring-boot:run
```

4. Execute o admin web:

```bash
cd admin-web
npm install
npm start
```

Credencial inicial de desenvolvimento:

```text
login: admin
senha: admin123
```

5. Execute o PDV desktop:

```bash
cd pdv-desktop
mvn javafx:run
```

## Verificacoes

```bash
cd backend-api && mvn test
cd admin-web && npm test -- --watch=false
cd pdv-desktop && mvn test
```

No ambiente usado na ultima verificacao completa registrada, `backend-api` passou em `mvn test` com 33 testes, `admin-web` passou em `npm test -- --watch=false --progress=false` com 56 specs e `pdv-desktop` passou em `mvn test` com 8 testes. Esta atualizacao de documentacao nao alterou codigo de aplicacao.

## Documentacao

- [Estado atual](docs/estado-atual.md)
- [Arquitetura](docs/arquitetura.md)
- [Backlog MVP](docs/backlog-mvp.md)
- [Desenvolvimento local](docs/desenvolvimento-local.md)
- [Testes e verificacao](docs/testes-e-verificacao.md)
- [Contratos de API](docs/api-contratos.md)
- [Dados e migracoes](docs/dados-e-migracoes.md)
- [Seguranca](docs/seguranca.md)
- [Diagramas UML e Mermaid](docs/diagramas.md)
- [Requisitos MVP](docs/requisitos-mvp-mercado-one.md)
- [Decisoes de stack e scaffold](docs/decisoes-stack-scaffold.md)
- [Guia para agentes](docs/projeto.AGENTS.md)
- [Manutencao por agentes](docs/manutencao.AGENTS.md)

Documentos especificos de uma area podem ficar no `README.md` do subprojeto ou em um diretorio `docs/` local. Documentos voltados especificamente para agentes usam o sufixo `.AGENTS.md`.
