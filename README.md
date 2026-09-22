# Mercado One

Mercado One e um ERP em scaffold funcional para pequenos mercados varejistas, com administrativo web, API Java e PDV desktop offline-first. O objetivo do repositorio e evoluir o MVP de loja fisica: cadastros, estoque simples, vendas presenciais, operacao offline parcial, clientes e relatorios operacionais.

O que ja esta no codigo: [Estado atual](docs/estado-atual.md). O que ainda falta: [Backlog MVP](docs/backlog-mvp.md). Uso da loja: [Admin web](docs/uso-admin-web.md) e [PDV](docs/uso-pdv.md).

A UI do PDV nao chama `POST /api/sales`. O caixa grava a venda localmente e sincroniza por `POST /api/offline/sales/sync`.

## Stack

- Backend: Java 25 LTS, Spring Boot 4.1.x, Maven, PostgreSQL 18, Flyway, JPA, Validation, Security e Actuator.
- Admin web: Angular 22, TypeScript, HTML e CSS.
- PDV desktop: Java 25 LTS, JavaFX 25 e SQLite local.
- Infra local: Docker Compose para PostgreSQL.

## Estrutura

```text
backend-api/   API REST e dominio servidor
admin-web/     Administrativo web Angular
pdv-desktop/   Cliente desktop JavaFX para PDV
infra/         Infraestrutura local de desenvolvimento
docs/          Guias transversais, contratos e decisoes
CONTEXT.md     Glossario de dominio
```

Componentes, fronteiras e fluxo de venda: [Arquitetura](docs/arquitetura.md).

## Desenvolvimento local

1. Copie `.env.example` para `.env` na raiz. A API exige `MERCADO_ONE_JWT_SECRET`.
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

`OPERADOR_CAIXA` e o perfil do PDV. O admin web autentica esse usuario, mas as rotas do shell sao de `ADMIN`, `GERENTE` e `ESTOQUISTA`.

5. Execute o PDV desktop:

```bash
cd pdv-desktop
mvn javafx:run
```

O campo de login do PDV vem preenchido com `operador`; use um usuario real cadastrado (o seed cria `admin`). A URL default da API no PDV e `http://localhost:8080`.

Detalhes de variaveis, portas e troubleshooting: [Desenvolvimento local](docs/desenvolvimento-local.md).

## Verificacoes

```bash
cd backend-api && mvn test
cd admin-web && npm test -- --watch=false
cd pdv-desktop && mvn test
```

Contagem no codigo-fonte, sem reexecutar a suite nesta atualizacao: 33 metodos `@Test` em `backend-api`, 56 specs `it(` em `admin-web` e 8 metodos `@Test` em `pdv-desktop`.

## Documentacao

Indice: [docs/README.md](docs/README.md).

- [Glossario de dominio](CONTEXT.md)
- [Estado atual](docs/estado-atual.md)
- [Uso do admin web](docs/uso-admin-web.md)
- [Uso do PDV](docs/uso-pdv.md)
- [Arquitetura](docs/arquitetura.md)
- [Backlog MVP](docs/backlog-mvp.md)
- [Desenvolvimento local](docs/desenvolvimento-local.md)
- [Testes e verificacao](docs/testes-e-verificacao.md)
- [Contratos de API](docs/api-contratos.md)
- [Dados e migracoes](docs/dados-e-migracoes.md)
- [Seguranca](docs/seguranca.md)
- [Offline PDV e sync](docs/offline-pdv-sync.md)
- [Diagramas](docs/diagramas.md)
- [Requisitos MVP](docs/requisitos-mvp-mercado-one.md)
- [Guia para agentes](docs/projeto.AGENTS.md)
- [Manutencao por agentes](docs/manutencao.AGENTS.md)
