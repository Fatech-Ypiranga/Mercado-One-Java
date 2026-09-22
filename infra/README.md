# Infraestrutura Local

Infraestrutura de desenvolvimento local do Mercado One. Nao ha compose de API, admin, PDV, Redis, fila ou ambiente de producao neste repositorio.

## Estado atual

O arquivo `docker-compose.yml` sobe um PostgreSQL 18 para a API durante o desenvolvimento. Nao ha compose de API, admin, PDV, Redis, fila ou ambiente de producao.

Imagem, banco, usuario, porta, volume e healthcheck estao em [Dados e migracoes](../docs/dados-e-migracoes.md). O container se chama `mercado-one-postgres`.

O Compose interpola variaveis a partir do diretorio `infra/` ou do ambiente do shell. O `.env` da raiz e lido pela API Spring, nao automaticamente por este Compose.

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

As variaveis ficam exemplificadas em `../.env.example` e descritas em [Dados e migracoes](../docs/dados-e-migracoes.md).

A API precisa de `MERCADO_ONE_DATABASE_URL` alinhada a host, porta e nome reais. JWT e seed de admin ficam no `.env` da raiz e nao neste Compose.

Para ambientes reais, use variaveis de ambiente ou secret manager. Nao reutilize as credenciais default de desenvolvimento.
