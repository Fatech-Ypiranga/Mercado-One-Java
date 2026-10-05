# Infraestrutura

Infraestrutura de desenvolvimento local do Mercado One e registro do piloto na Azure. Não há compose de API, admin, PDV, Redis ou fila neste repositório.

## Estado atual

O arquivo `docker-compose.yml` sobe um PostgreSQL 18 para a API durante o desenvolvimento. O piloto na Azure está descrito abaixo; seus recursos não são provisionados por este Compose.

Imagem, banco, usuário, porta, volume e healthcheck estão em [Dados e migrações](../docs/dados-e-migracoes.md). O container se chama `mercado-one-postgres`.

O Compose interpola variáveis a partir do diretório `infra/` ou do ambiente do shell. O `.env` da raiz é lido pela API Spring, não automaticamente por este Compose.

## Comandos

Na raiz do repositório:

```bash
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml ps
docker compose -f infra/docker-compose.yml down
```

Para remover também o volume local:

```bash
docker compose -f infra/docker-compose.yml down -v
```

Use `down -v` com cuidado, pois ele apaga os dados locais do PostgreSQL.

O Compose interpola variáveis a partir do project directory (`infra/`) ou do ambiente do shell. O `.env` da raiz é lido pela API Spring, não automaticamente por este Compose.

## Variáveis

As variáveis ficam exemplificadas em `../.env.example` e descritas em [Dados e migrações](../docs/dados-e-migracoes.md).

A API precisa de `MERCADO_ONE_DATABASE_URL` alinhada ao host, à porta e ao nome reais. JWT e seed de admin ficam no `.env` da raiz e não neste Compose.

Para ambientes reais, use variáveis de ambiente ou secret manager. Não reutilize as credenciais default de desenvolvimento.

## Piloto na Azure

Assinatura Azure for Students, grupo `mercado-one-prod`. A política da assinatura só permite `brazilsouth`, `northcentralus`, `spaincentral`, `canadacentral` e `centralus`. Static Web Apps não existe em Brazil South, então o admin ficou em Central US; o conteúdo é servido pela CDN.

- API: `https://mercado-one-api-935d.azurewebsites.net` — App Service Linux B1, Java 25, Brazil South.
- Banco: `mercado-one-pg-935d.postgres.database.azure.com` — PostgreSQL 18 Flexible Server Burstable B1ms, 32 GB, Brazil South. Firewall aberto para serviços Azure.
- Admin: `https://zealous-sand-0354e3510.6.azurestaticapps.net` — Static Web Apps Free.

Segredos ficam nas App Settings da API (`MERCADO_ONE_JWT_SECRET`, senha do banco, `MERCADO_ONE_DATABASE_URL` com `sslmode=require`). `MERCADO_ONE_COOKIE_SECURE=true` e o CORS aponta para o host do admin. O seed de admin foi desligado depois da primeira subida.

Publicar de novo:

```bash
cd backend-api
mvn -DskipTests package
az webapp deploy --resource-group mercado-one-prod --name mercado-one-api-935d --src-path target/backend-api-0.1.0-SNAPSHOT.jar --type jar

cd ../admin-web
MERCADO_ONE_API_BASE_URL=https://mercado-one-api-935d.azurewebsites.net npm run build:production
npx @azure/static-web-apps-cli deploy ../output/admin-web-dist/browser --env production --app-name mercado-one-admin-935d --resource-group mercado-one-prod
```

O PDV continua no caixa. No campo de API, use `https://mercado-one-api-935d.azurewebsites.net`.
