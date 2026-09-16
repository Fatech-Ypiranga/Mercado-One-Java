# Infraestrutura Local

Infraestrutura de desenvolvimento local do Mercado One.

## Estado atual

O arquivo `docker-compose.yml` sobe um PostgreSQL 18 para uso da API durante desenvolvimento. As credenciais locais sao configuradas por variaveis de ambiente com defaults de desenvolvimento.

Servico atual:

- `postgres`: container `mercado-one-postgres`.
- Banco default: `mercado_one`.
- Usuario default: `mercado_one`.
- Porta default: `5432`.
- Volume persistente: `postgres18-data`.
- Healthcheck via `pg_isready`.

## Comandos

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

## Variaveis

As variaveis ficam exemplificadas em `../.env.example`:

- `MERCADO_ONE_DB_NAME`
- `MERCADO_ONE_DB_USER`
- `MERCADO_ONE_DB_PASSWORD`
- `MERCADO_ONE_DB_PORT`
- `MERCADO_ONE_API_PORT`

Para ambientes reais, use variaveis de ambiente ou secret manager. Nao reutilize as credenciais default de desenvolvimento.
