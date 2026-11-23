# Publicar no Render

O Blueprint da raiz (`render.yaml`) cria a API Spring Boot, um PostgreSQL e o painel Angular como serviços separados. O PDV JavaFX continua sendo um aplicativo desktop e não é hospedado pelo Render.

## Antes de começar

1. Envie o projeto para um repositório GitHub que você controle. Não envie `.env`; ele já está ignorado pelo Git. Nunca copie os valores desse arquivo para o Blueprint ou para o repositório.
2. No Render, escolha **New > Blueprint**, conecte o repositório e selecione o branch que contém `render.yaml`.
3. Confira os três recursos propostos e confirme a criação. API e PostgreSQL usam o plano Free na região Ohio; o painel estático é servido pela CDN global do Render.
4. Antes de testar o painel, abra as variáveis do serviço `mercado-one-api`, defina um valor forte e particular para `MERCADO_ONE_SEED_ADMIN_PASSWORD` e salve. O usuário inicial é `admin`; a senha é o valor que você definiu. Não use a senha de desenvolvimento local.
5. Aguarde o deploy da API ficar saudável em `/actuator/health` e depois o deploy do painel. As migrations Flyway rodam no início da API.

Se o Render atribuir um domínio diferente dos nomes `mercado-one-api` ou `mercado-one-admin`, ajuste `MERCADO_ONE_API_BASE_URL` no serviço estático e `MERCADO_ONE_CORS_ALLOWED_ORIGINS` no serviço da API para os domínios exibidos no Dashboard; salve e faça novo deploy dos serviços afetados.

## Redefinição de senha por e-mail (opcional)

Sem configuração, a tela **Esqueci a senha** orienta falar com o administrador do mercado, e nenhum e-mail sai. Para ligar o envio, no serviço `mercado-one-api` defina `MERCADO_ONE_MAIL_HOST`, `MERCADO_ONE_MAIL_USER`, `MERCADO_ONE_MAIL_PASSWORD` e `MERCADO_ONE_MAIL_FROM` (e `MERCADO_ONE_MAIL_PORT`, padrão 587) com os dados do seu provedor de SMTP, e confira `MERCADO_ONE_APP_URL` (o endereço público do painel, usado no link). O envio só liga quando host **e** remetente estão preenchidos. Detalhes e variáveis em [Desenvolvimento local](desenvolvimento-local.md#redefinição-de-senha-por-e-mail).

## Canais de suporte do painel (opcional)

A tela **Fale com o suporte** do painel mostra os canais definidos no build. No serviço estático `mercado-one-admin`, adicione as variáveis de ambiente `MERCADO_ONE_SUPPORT_WHATSAPP` (só dígitos, com 55 e DDD), `MERCADO_ONE_SUPPORT_EMAIL` e `MERCADO_ONE_SUPPORT_HOURS` e refaça o deploy. Sem elas, a tela avisa que os canais ainda não foram configurados. Detalhes em [Desenvolvimento local](desenvolvimento-local.md#build-de-produção).

## Para apresentar

- Abra `https://mercado-one-admin.onrender.com` e entre com `admin` e a senha configurada no passo anterior.
- Se a API tiver ficado ociosa, a primeira chamada pode demorar enquanto o serviço Free inicia; abra também `https://mercado-one-api.onrender.com/actuator/health` antes de demonstrar e espere a resposta de saúde.
- Cadastros e vendas ficam no PostgreSQL, não no disco temporário do serviço.

## Limitações do plano Free

Serviços web Free podem entrar em suspensão após 15 minutos sem tráfego e levar cerca de um minuto para voltar. O PostgreSQL Free expira 30 dias após a criação. Esse plano serve para a apresentação e testes, não para produção nem para guardar dados importantes a longo prazo.

## Se o deploy falhar

- **API não inicia:** confira os logs do serviço, o health check e se as variáveis PostgreSQL vieram do banco `mercado-one-db`.
- **Erro de banco:** confirme que API e PostgreSQL estão na mesma região e que o banco foi criado.
- **Login funciona, mas as telas não carregam dados:** confirme que os domínios configurados em `MERCADO_ONE_API_BASE_URL` e `MERCADO_ONE_CORS_ALLOWED_ORIGINS` são exatamente os domínios públicos atuais, com `https://`.
- **Painel retorna 404 ao atualizar uma rota:** confirme que a regra de rewrite para `/index.html` está presente no serviço estático.
