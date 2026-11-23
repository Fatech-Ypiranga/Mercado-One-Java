# Mercado One

Mercado One é um ERP em scaffold funcional para pequenos mercados varejistas, com administrativo web, API Java e PDV desktop offline-first. O objetivo do repositório é evoluir o MVP de loja física: cadastros, estoque simples, vendas presenciais, operação offline parcial, clientes e relatórios operacionais.

Entregue e pendente: [Backlog MVP](docs/produto/backlog-mvp.md). Uso da loja: [Admin web](docs/produto/uso-admin-web.md) e [PDV](docs/produto/uso-pdv.md).

O PDV confirma online por `POST /api/sales` (idempotente por `clientSaleId`) depois de gravar a venda no SQLite; sem API, a venda fica na fila e sincroniza por `POST /api/offline/sales/sync`.

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
site/          Página institucional (HTML estático)
infra/         Infraestrutura local de desenvolvimento
docs/          Guias por assunto: produto, arquitetura, desenvolvimento, dados, marca
CONTEXT.md     Glossário de domínio
```

Componentes, fronteiras e fluxo de venda: [Arquitetura](docs/arquitetura/arquitetura.md).

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

O PDV abre numa tela de entrada, com o último login usado no computador; use um usuário real cadastrado (o seed cria `admin`). Sem internet, só entra quem já entrou online naquele caixa nos últimos 30 dias. A URL padrão da API no PDV é `http://localhost:8080`.

Detalhes de variáveis, portas e troubleshooting: [Desenvolvimento local](docs/desenvolvimento/desenvolvimento-local.md).

## Publicação no Render

O arquivo `render.yaml` configura a API, o PostgreSQL e o painel web. Consulte [Publicar no Render](docs/desenvolvimento/deploy-render.md) para conectar o repositório e concluir a publicação.

## Verificações

```bash
cd backend-api && mvn test
cd admin-web && npm test -- --watch=false
cd pdv-desktop && mvn test
```

O CI (`.github/workflows/ci.yml`) executa as três suítes em cada pull request e push na `main`; o backend roda também a validação das migrations em PostgreSQL via Testcontainers. Localmente, `npm test` no admin precisa de Chrome ou da variável `CHROME_BIN` apontando para um navegador Chromium.

## Documentação

Índice: [docs/README.md](docs/README.md).

- [Glossário de domínio](CONTEXT.md)
- [Uso do admin web](docs/produto/uso-admin-web.md)
- [Uso do PDV](docs/produto/uso-pdv.md)
- [Arquitetura](docs/arquitetura/arquitetura.md)
- [Backlog MVP](docs/produto/backlog-mvp.md)
- [Desenvolvimento local](docs/desenvolvimento/desenvolvimento-local.md)
- [Testes e verificação](docs/desenvolvimento/testes-e-verificacao.md)
- [Contratos de API](docs/desenvolvimento/api-contratos.md)
- [Dados e migrações](docs/dados/dados-e-migracoes.md)
- [Segurança](docs/arquitetura/seguranca.md)
- [Offline PDV e sync](docs/arquitetura/offline-pdv-sync.md)
- [Diagramas](docs/arquitetura/diagramas.md)
- [Requisitos MVP](docs/produto/requisitos-mvp-mercado-one.md)
- [Guia para agentes](docs/agentes/projeto.AGENTS.md)
- [Manutenção por agentes](docs/agentes/manutencao.AGENTS.md)
