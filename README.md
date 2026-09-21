# Mercado One

Mercado One e um ERP em scaffold funcional para pequenos mercados varejistas, com administrativo web, API Java e PDV desktop offline-first. O objetivo do repositorio e evoluir o MVP de loja fisica: cadastros, estoque simples, vendas presenciais, operacao offline parcial, clientes e relatorios operacionais.

## Estado atual

Este repositorio esta em fase de scaffold funcional. A stack, a estrutura de modulos e os contratos HTTP ja estao definidos, e as principais fatias operacionais do MVP ja existem em codigo. O backlog diferencia funcionalidades entregues de refinamentos para piloto.

Implementado agora:

- Backend Spring Boot com envelope HTTP padronizado, handler global de erro, autenticacao JWT/cookie HttpOnly, seed de administrador, autorizacao por perfil, gestao de usuarios/perfis, catalogo de categorias/produtos, clientes, fornecedores, estoque simples, venda confirmada no servidor, relatorio de vendas paginado/CSV, produtos mais vendidos, sync offline, conflitos offline resoluveis, auditoria inicial (gravacao) e `GET /api/system/info`.
- Admin Angular com login, sessao por cookie HttpOnly, shell protegido, guards por perfil, dashboard inicial e telas funcionais de usuarios, categorias, produtos, estoque, vendas, conflitos offline, clientes e fornecedores.
- PDV JavaFX com login de operador, busca online de produtos/clientes, fallback de catalogo local SQLite, carrinho, comprovante simples nao fiscal, fila SQLite local e sincronizacao via `POST /api/offline/sales/sync`. Toda finalizacao no PDV grava a venda localmente antes de tentar a rede.
- Infra local com PostgreSQL 18 via Docker Compose.
- Testes minimos nos tres subprojetos.

Planejado para o MVP / piloto:

- Refinamentos de UX para operacao piloto.
- Padronizacao completa de mensagens e tratamento visual de erro em todas as telas.
- Relatorios adicionais alem dos operacionais ja implementados.
- Consulta administrativa de auditoria (hoje so ha gravacao).
- Desconto simples no PDV, citado nos requisitos conceituais e ainda nao implementado.
- Dados fiscais apenas preparatorios, sem emissao fiscal no MVP.

O contrato `POST /api/sales` existe na API para venda confirmada com preco vigente do servidor. A UI do PDV nao chama esse endpoint: o fluxo de caixa sempre usa fila local + sync offline.

## Stack

- Backend: Java 25 LTS, Spring Boot 4.1.x, Maven, PostgreSQL 18, Flyway, JPA, Validation, Security e Actuator.
- Admin web: Angular 22, TypeScript, HTML e CSS.
- PDV desktop: Java 25 LTS, JavaFX 25 e SQLite local.
- Infra local: Docker Compose para PostgreSQL.

## Estrutura

```text
backend-api/   API REST e dominio servidor
admin-web/     Administrativo web Angular
pdv-desktop/   Cliente desktop JavaFX para PDV
infra/         Infraestrutura local de desenvolvimento
docs/          Requisitos, arquitetura, backlog e guias transversais
CONTEXT.md     Glossario de dominio
```

## Arquitetura

O backend usa fatias verticais por capacidade de negocio. Cada modulo concentra sua propria interface HTTP, casos de uso, dominio e adaptadores internos.

```text
backend-api/src/main/java/com/mercadoone/backend/modules/
  access/
  catalog/
  customer/
  inventory/
  sales/
  offline/
  supplier/
  audit/
  system/
```

Contratos HTTP compartilhados ficam em `api/common`; configuracoes transversais ficam em `infrastructure`. A intencao e que modulos nao dependam de detalhes internos de outros modulos; no codigo atual ainda ha acoplamento direto entre algumas fatias (por exemplo `sales` usa `Catalog`/`Customer`/`Inventory`, e `inventory` usa entidade `Product`).

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

`OPERADOR_CAIXA` e o perfil do PDV. O admin web autentica esse usuario, mas as rotas do shell sao de `ADMIN`, `GERENTE` e `ESTOQUISTA`.

5. Execute o PDV desktop:

```bash
cd pdv-desktop
mvn javafx:run
```

O campo de login do PDV vem preenchido com `operador`; use um usuario real cadastrado (o seed cria `admin`). A URL default da API no PDV e `http://localhost:8080`.

Detalhes de variaveis, portas e troubleshooting: [Desenvolvimento local](docs/desenvolvimento-local.md).

## Verificacoes

```bash
cd backend-api && mvn test
cd admin-web && npm test -- --watch=false
cd pdv-desktop && mvn test
```

Contagem no codigo-fonte desta atualizacao: 33 metodos `@Test` em `backend-api`, 56 specs `it(` em `admin-web` e 8 metodos `@Test` em `pdv-desktop`. A ultima execucao registrada na documentacao passou com esses totais. Esta atualizacao de documentacao nao reexecutou a suite e nao alterou codigo de aplicacao.

## Documentacao

Indice completo: [docs/README.md](docs/README.md).

- [Glossario de dominio](CONTEXT.md)
- [Estado atual](docs/estado-atual.md)
- [Arquitetura](docs/arquitetura.md)
- [Backlog MVP](docs/backlog-mvp.md)
- [Desenvolvimento local](docs/desenvolvimento-local.md)
- [Testes e verificacao](docs/testes-e-verificacao.md)
- [Contratos de API](docs/api-contratos.md)
- [Dados e migracoes](docs/dados-e-migracoes.md)
- [Seguranca](docs/seguranca.md)
- [Offline PDV e sync](docs/offline-pdv-sync.md)
- [Diagramas UML e Mermaid](docs/diagramas.md)
- [Requisitos MVP](docs/requisitos-mvp-mercado-one.md)
- [Decisoes de stack e scaffold](docs/decisoes-stack-scaffold.md)
- [Guia para agentes](docs/projeto.AGENTS.md)
- [Manutencao por agentes](docs/manutencao.AGENTS.md)

Documentos especificos de uma area podem ficar no `README.md` do subprojeto ou em um diretorio `docs/` local. Documentos voltados especificamente para agentes usam o sufixo `.AGENTS.md`.
