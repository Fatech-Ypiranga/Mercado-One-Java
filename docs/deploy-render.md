# Publicar no Render

O Blueprint da raiz (`render.yaml`) cria a API Spring Boot, um PostgreSQL e o painel Angular como serviços separados. O PDV JavaFX continua sendo um aplicativo desktop e não é hospedado pelo Render.

## Antes de começar

1. Envie o projeto para um repositório GitHub que você controle. Não envie `.env`; ele já está ignorado pelo Git. Nunca copie os valores desse arquivo para o Blueprint ou para o repositório.
2. No Render, escolha **New > Blueprint**, conecte o repositório e selecione o branch que contém `render.yaml`.
3. Confira os três recursos propostos e confirme a criação. API e PostgreSQL usam o plano Free na região Ohio; o painel estático é servido pela CDN global do Render.
4. Antes de testar o painel, abra as variáveis do serviço `mercado-one-api`, defina um valor forte e particular para `MERCADO_ONE_SEED_ADMIN_PASSWORD` e salve. O usuário inicial é `admin`; a senha é o valor que você definiu. Não use a senha de desenvolvimento local.
5. Aguarde o deploy da API ficar saudável em `/actuator/health` e depois o deploy do painel. As migrations Flyway rodam no início da API.

Se o Render atribuir um domínio diferente dos nomes `mercado-one-api` ou `mercado-one-admin`, ajuste `MERCADO_ONE_API_BASE_URL` no serviço estático e `MERCADO_ONE_CORS_ALLOWED_ORIGINS` no serviço da API para os domínios exibidos no Dashboard; salve e faça novo deploy dos serviços afetados.

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
