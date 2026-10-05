# Mercado One

Mercado One é um ERP em scaffold funcional para pequenos mercados varejistas, com administrativo web, API Java e PDV desktop offline-first. O objetivo do repositório é evoluir o MVP de loja física: cadastros, estoque simples, vendas presenciais, operação offline parcial, clientes e relatórios operacionais.

O que já está no código: [Estado atual](docs/estado-atual.md). O que ainda falta: [Backlog MVP](docs/backlog-mvp.md). Uso da loja: [Admin web](docs/uso-admin-web.md) e [PDV](docs/uso-pdv.md).

A UI do PDV não chama `POST /api/sales`. O caixa grava a venda localmente e sincroniza por `POST /api/offline/sales/sync`.

## Stack

- Backend: Java 25 LTS, Spring Boot 4.1.x, Maven, PostgreSQL 18, Flyway, JPA, Validation, Security e Actuator.
- Admin web: Angular 22, TypeScript, HTML e CSS.
- PDV desktop: Java 25 LTS, JavaFX 25 e SQLite local.
- Infra local: Docker Compose para PostgreSQL.

## Estrutura

```text
backend-api/   API REST e domínio servidor
admin-web/     Administrativo web Angular
pdv-desktop/   Cliente desktop JavaFX para PDV
infra/         Infraestrutura local de desenvolvimento
docs/          Guias transversais, contratos e decisões
CONTEXT.md     Glossário de domínio
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

`OPERADOR_CAIXA` é o perfil do PDV. O admin web autentica esse usuário, mas as rotas do shell são de `ADMIN`, `GERENTE` e `ESTOQUISTA`.

5. Execute o PDV desktop:

```bash
cd pdv-desktop
mvn javafx:run
```

O campo de login do PDV vem preenchido com `operador`; use um usuário real cadastrado (o seed cria `admin`). A URL padrão da API no PDV é `http://localhost:8080`.

Detalhes de variáveis, portas e troubleshooting: [Desenvolvimento local](docs/desenvolvimento-local.md).

## Verificações

```bash
cd backend-api && mvn test
cd admin-web && npm test -- --watch=false
cd pdv-desktop && mvn test
```

Contagem no código-fonte, sem reexecutar a suíte nesta atualização: 33 métodos `@Test` em `backend-api`, 56 specs `it(` em `admin-web` e 8 métodos `@Test` em `pdv-desktop`.

## Documentação

Índice: [docs/README.md](docs/README.md).

- [Glossário de domínio](CONTEXT.md)
- [Estado atual](docs/estado-atual.md)
- [Uso do admin web](docs/uso-admin-web.md)
- [Uso do PDV](docs/uso-pdv.md)
- [Arquitetura](docs/arquitetura.md)
- [Backlog MVP](docs/backlog-mvp.md)
- [Desenvolvimento local](docs/desenvolvimento-local.md)
- [Testes e verificação](docs/testes-e-verificacao.md)
- [Contratos de API](docs/api-contratos.md)
- [Dados e migrações](docs/dados-e-migracoes.md)
- [Segurança](docs/seguranca.md)
- [Offline PDV e sync](docs/offline-pdv-sync.md)
- [Diagramas](docs/diagramas.md)
- [Requisitos MVP](docs/requisitos-mvp-mercado-one.md)
- [Guia para agentes](docs/projeto.AGENTS.md)
- [Manutenção por agentes](docs/manutencao.AGENTS.md)
