# Infraestrutura Local

Infraestrutura de desenvolvimento local do Mercado One. Nao ha compose de API, admin, PDV, Redis, fila ou ambiente de producao neste repositorio.

## Estado atual

O arquivo `docker-compose.yml` sobe um PostgreSQL 18 para uso da API durante desenvolvimento. As credenciais locais sao configuradas por variaveis de ambiente com defaults de desenvolvimento.

Servico atual:

- `postgres`: container `mercado-one-postgres`.
- Imagem: `postgres:18`.
- Banco default: `mercado_one`.
- Usuario default: `mercado_one`.
- Senha default: `mercado_one_dev`.
- Porta default: `5432`.
- Volume persistente: `postgres18-data`.
- Healthcheck via `pg_isready`.

## Comandos

Na raiz do repositorio:

```bash
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml down
```

Para remover tambem o volume local:

```bash
docker compose -f infra/docker-compose.yml down -v
```

Use `down -v` com cuidado, pois ele apaga os dados locais do PostgreSQL.

O Compose interpola variaveis a partir do project directory (`infra/`) ou do ambiente do shell. O `.env` da raiz e lido pela API Spring, nao automaticamente por este Compose.

## Variaveis

As variaveis ficam exemplificadas em `../.env.example`:

- `MERCADO_ONE_DB_NAME`
- `MERCADO_ONE_DB_USER`
- `MERCADO_ONE_DB_PASSWORD`
- `MERCADO_ONE_DB_PORT`
- `MERCADO_ONE_API_PORT` (nao usada pelo Compose; documentada junto porque pertence ao setup local)

A API precisa de `MERCADO_ONE_DATABASE_URL` alinhada a host/porta/nome reais. JWT e seed de admin tambem ficam no `.env` da raiz e nao neste Compose.

Para ambientes reais, use variaveis de ambiente ou secret manager. Nao reutilize as credenciais default de desenvolvimento.
