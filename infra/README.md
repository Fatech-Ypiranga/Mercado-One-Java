# Infraestrutura

Infraestrutura de desenvolvimento local do Mercado One e registro do piloto na Azure. Não há compose de API, admin, PDV, Redis ou fila neste repositório.

## Estado atual

O arquivo `docker-compose.yml` sobe um PostgreSQL 18 para a API durante o desenvolvimento. O piloto na Azure está descrito abaixo; seus recursos não são provisionados por este Compose.

Imagem, banco, usuário, porta, volume e healthcheck estão em [Dados e migrações](../docs/dados/dados-e-migracoes.md). O container se chama `mercado-one-postgres`.

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

As variáveis ficam exemplificadas em `../.env.example` e descritas em [Dados e migrações](../docs/dados/dados-e-migracoes.md).

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

### Redefinição de senha por e-mail na Azure

O envio fica **desligado** enquanto `MERCADO_ONE_MAIL_HOST` e `MERCADO_ONE_MAIL_FROM` não estiverem nas App Settings da API; nesse caso a tela "Esqueci a senha" orienta falar com o administrador. A API não envia e-mail por conta própria: ela fala com um servidor SMTP, e a Azure não oferece um SMTP solto. A opção da própria Azure é o **E-mail do Azure Communication Services** (ACS), que aceita SMTP em `smtp.azurecomm.net`, porta 587, com TLS (STARTTLS). O Static Web Apps já devolve o `index.html` para qualquer rota (`admin-web/public/staticwebapp.config.json`), então o link `/nova-senha?token=...` do e-mail abre direto.

Passos, uma vez só:

1. No portal, crie um recurso **Communication Services** e, nele, um recurso **Email Communication Services** com um domínio (o domínio gerenciado pela Azure, `xxxx.azurecomm.net`, serve para o piloto; domínio próprio exige verificar DNS). Conecte o domínio ao recurso de Communication Services. O remetente é `DoNotReply@<domínio>`.
2. Crie uma **credencial SMTP**: um aplicativo no Microsoft Entra com um segredo de cliente, ligado ao recurso de Communication Services (aba **SMTP Usernames**). O usuário SMTP é o nome que você definir e a senha é o segredo do aplicativo. Siga o passo a passo atual da Microsoft, que mudou de formato mais de uma vez: [Criar credenciais de autenticação para SMTP](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/email/send-email-smtp/smtp-authentication) e [Enviar e-mail por SMTP](https://learn.microsoft.com/en-us/azure/communication-services/quickstarts/email/send-email-smtp/send-email-smtp).
3. Grave as variáveis na API (substitua os valores entre `<>`). Prefira digitar a senha num terminal que não guarde histórico, ou referenciar um Key Vault:

```bash
az webapp config appsettings set --resource-group mercado-one-prod --name mercado-one-api-935d --settings \
  MERCADO_ONE_APP_URL=https://zealous-sand-0354e3510.6.azurestaticapps.net \
  MERCADO_ONE_MAIL_HOST=smtp.azurecomm.net \
  MERCADO_ONE_MAIL_PORT=587 \
  MERCADO_ONE_MAIL_STARTTLS=true \
  MERCADO_ONE_MAIL_USER="<usuário SMTP>" \
  MERCADO_ONE_MAIL_PASSWORD="<segredo do aplicativo Entra>" \
  MERCADO_ONE_MAIL_FROM="DoNotReply@<domínio>.azurecomm.net"
```

4. **Publique a API de novo** (comando acima): a versão atual traz a migração `V9`, que acrescenta a coluna `email` em `app_users` e a tabela `password_reset_tokens`. Ela só acrescenta, não apaga dados, e roda sozinha na subida. Publique também o admin, que ganhou as telas `/esqueci-senha` e `/nova-senha`.
5. Em **Usuários**, cadastre o e-mail de cada pessoa. Só recebe o link quem tem e-mail cadastrado.
6. Teste com um usuário seu: **Esqueci a senha** pede o link, o e-mail chega, o link funciona uma vez.

Pontos de atenção: o recurso de e-mail da Azure tem limite de envios por minuto e por hora, bem acima do que um mercado usa; a porta 25 não é aberta a partir do App Service, por isso a 587; e a mensagem de redefinição pode cair na caixa de spam até o domínio ter reputação, o que a tela já avisa.
