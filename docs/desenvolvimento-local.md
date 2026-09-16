# Desenvolvimento Local

Este guia descreve o setup esperado para executar o Mercado One localmente.

## Pre-requisitos

- Java 25.
- Maven disponivel no PATH.
- Node.js compativel com Angular 22.
- npm funcional.
- Docker com suporte a Docker Compose.

## Variaveis

Use `.env.example` como referencia:

```env
MERCADO_ONE_DB_NAME=mercado_one
MERCADO_ONE_DB_USER=mercado_one
MERCADO_ONE_DB_PASSWORD=mercado_one_dev
MERCADO_ONE_DB_PORT=5432
MERCADO_ONE_API_PORT=8080
```

As credenciais default sao apenas para desenvolvimento local.

## Banco local

Na raiz do projeto:

```bash
docker compose -f infra/docker-compose.yml up -d
```

Verifique o container:

```bash
docker compose -f infra/docker-compose.yml ps
```

## API

```bash
cd backend-api
mvn spring-boot:run
```

Endpoints uteis:

- `http://localhost:8080/actuator/health`
- `http://localhost:8080/api/system/info`

## Admin web

```bash
cd admin-web
npm install
npm start
```

O admin web roda em `http://localhost:4200` e usa `http://localhost:8080` como API local.

## PDV desktop

```bash
cd pdv-desktop
mvn javafx:run
```

O caminho local default para o SQLite do PDV e `~/.mercado-one/pdv-offline.sqlite3`.

## Troubleshooting

- Se `mvn` nao for reconhecido, instale Maven ou ajuste o PATH.
- Se `npm` falhar com `npm-cli.js` ausente, repare a instalacao do Node/npm antes de rodar os comandos Angular.
- Se a API nao conectar no banco, confirme se o container `mercado-one-postgres` esta saudavel e se as variaveis batem com o `docker-compose.yml`.
- Se a porta `5432` ou `8080` estiver ocupada, ajuste `MERCADO_ONE_DB_PORT` ou `MERCADO_ONE_API_PORT`.
