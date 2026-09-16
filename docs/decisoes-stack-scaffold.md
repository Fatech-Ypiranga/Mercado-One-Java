# Decisoes de stack e scaffold

As decisoes estruturais tambem estao registradas como ADRs curtos em `docs/adr/`.

## Decisoes travadas

- Monorepo com `backend-api`, `admin-web`, `pdv-desktop` e `infra`.
- Backend em Java 25 LTS com Spring Boot 4.1.x, Maven, PostgreSQL 18 e Flyway.
- Administrativo web em Angular 22.
- PDV offline em Java desktop com JavaFX e SQLite local.
- Banco servidor relacional em PostgreSQL; banco local do PDV restrito a catalogo e fila offline.
- Arquitetura por fatias verticais no backend, com modulos de negocio em `modules/`.

## Escopo do scaffold

O scaffold entrega uma base minima executavel. Novas fatias funcionais devem continuar pequenas e verificaveis conforme cadastros, venda online, fila offline e sincronizacao evoluem.

## Arquitetura do backend

O backend segue fatias verticais por capacidade de negocio, nao camadas tecnicas globais. Cada modulo evolui com sua propria `api`, `application`, `domain` e `infrastructure` internas quando precisar.

```text
modules/
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

Regras:

- `api/common` guarda somente contratos HTTP compartilhados, como envelope e erro padronizado.
- `infrastructure` na raiz guarda apenas configuracoes transversais, como seguranca e observabilidade.
- Modulos nao acessam detalhes internos de outros modulos.
- Integracoes entre modulos devem passar por interfaces pequenas de aplicacao, contratos HTTP, eventos ou consultas explicitamente publicadas.

## Contratos iniciais

- Envelope de API com `success`, `data`, `error` e `timestamp`.
- Erro padronizado com `code`, `message` e `details`.
- Perfis iniciais: `ADMIN`, `GERENTE`, `OPERADOR_CAIXA`, `ESTOQUISTA`.
- Endpoint publico `GET /api/system/info` para validar integracao entre admin web e API.
- Endpoint publico `GET /actuator/health` para health check.
