# Desenvolvimento Local

Este guia descreve o setup esperado para executar o Mercado One localmente.

## Pré-requisitos

- Java 25.
- Maven disponível no PATH.
- Node.js compatível com Angular 22.
- npm funcional.
- Docker com suporte a Docker Compose.

## Variáveis

Copie `.env.example` para `.env` na raiz do repositório. A API carrega esse arquivo ao subir com `mvn spring-boot:run` (diretório atual ou raiz do repo), via `spring.config.import` em `backend-api/src/main/resources/application.yml`. Sem `MERCADO_ONE_JWT_SECRET`, o Maven termina com `BUILD FAILURE`.

```env
MERCADO_ONE_DB_NAME=mercado_one
MERCADO_ONE_DB_USER=mercado_one
MERCADO_ONE_DB_PASSWORD=mercado_one_dev
MERCADO_ONE_DB_PORT=5432
MERCADO_ONE_API_PORT=8080
MERCADO_ONE_CORS_ALLOWED_ORIGINS=http://localhost:4200,http://127.0.0.1:4200
MERCADO_ONE_COOKIE_SECURE=false
MERCADO_ONE_JWT_SECRET=mercado-one-dev-secret-change-me-with-at-least-32-characters
MERCADO_ONE_JWT_EXPIRATION_MINUTES=480
MERCADO_ONE_SEED_ADMIN_ENABLED=true
MERCADO_ONE_SEED_ADMIN_NAME=Administrador
MERCADO_ONE_SEED_ADMIN_LOGIN=admin
MERCADO_ONE_SEED_ADMIN_PASSWORD=admin123
```

A API não lê `MERCADO_ONE_DB_NAME` nem `MERCADO_ONE_DB_PORT` diretamente. O JDBC default é:

```text
MERCADO_ONE_DATABASE_URL=jdbc:postgresql://localhost:5432/mercado_one
```

Se mudar nome ou porta do banco no Compose, ajuste também `MERCADO_ONE_DATABASE_URL` no `.env`. Essa chave não está no `.env.example` porque o default da API coincide com os defaults do Compose.

As credenciais default são apenas para desenvolvimento local. O seed só roda quando `MERCADO_ONE_SEED_ADMIN_ENABLED=true`.

## Banco local

Na raiz do projeto:

```bash
docker compose -f infra/docker-compose.yml up -d
```

O Compose usa o diretório de `infra/docker-compose.yml` como project directory. Interpolação `${MERCADO_ONE_*}` vem do ambiente do shell, de `infra/.env` se existir, ou dos defaults no YAML. O `.env` da raiz alimenta a API, não o Compose, salvo se as variáveis já estiverem no shell.

Verifique o container:

```bash
docker compose -f infra/docker-compose.yml ps
```

## API

```bash
cd backend-api
mvn spring-boot:run
```

Endpoints úteis:

- `http://localhost:8080/actuator/health` (público)
- `http://localhost:8080/api/system/info` (público)

`GET /actuator/info` exige autenticação. CORS da API cobre `/api/**` para as origens de `MERCADO_ONE_CORS_ALLOWED_ORIGINS`. O default local é `http://localhost:4200` e `http://127.0.0.1:4200`. A porta HTTP da API é `PORT`, se existir, senão `MERCADO_ONE_API_PORT` (default `8080`).

## Admin web

```bash
cd admin-web
npm install
npm start
```

O admin web escuta em `http://localhost:4200` (`--host 0.0.0.0 --port 4200`) e chama `http://localhost:8080` com credenciais (cookie). Login seed: `admin` / `admin123`.

Build:

```bash
npm run build
```

Saída em `output/admin-web-dist` (pasta `output/` ignorada pelo git). Cache do CLI em `output/angular-cache`.

`npm run build` usa `apiBaseUrl` vazio. Para um admin publicado fora da origem da API:

```bash
MERCADO_ONE_API_BASE_URL=https://exemplo.azurewebsites.net npm run build:production
```

## PDV desktop

```bash
cd pdv-desktop
mvn javafx:run
```

- URL default da API na tela: `http://localhost:8080`.
- Campo de login default: `operador` (placeholder; o seed cria `admin`).
- SQLite local: `~/.mercado-one/pdv-offline.sqlite3`.
- Finalizar sempre grava na fila local e chama `POST /api/offline/sales/sync`.

## Troubleshooting

- Se `mvn` não for reconhecido, instale Maven ou ajuste o PATH.
- Se `npm` falhar com `npm-cli.js` ausente, repare a instalação do Node/npm antes de rodar os comandos Angular.
- Se `mvn spring-boot:run` falhar com `Could not resolve placeholder 'MERCADO_ONE_JWT_SECRET'`, confirme se `.env` existe na raiz e contém essa chave.
- Se a API não conectar no banco, confirme se o container `mercado-one-postgres` está saudável e se `MERCADO_ONE_DATABASE_URL` bate com host/porta/nome reais.
- Se a porta `5432` ou `8080` estiver ocupada, ajuste `MERCADO_ONE_DB_PORT` **e** `MERCADO_ONE_DATABASE_URL`, ou `MERCADO_ONE_API_PORT`.
- Se o admin autenticar mas as chamadas seguintes falharem com CORS, use origem `http://localhost:4200` (ou `127.0.0.1:4200`) e mantenha a API em `8080`.
