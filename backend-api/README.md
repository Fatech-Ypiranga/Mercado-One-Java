# Backend API

API REST do Mercado One, responsavel pelos contratos de servidor, regras centrais do MVP e persistencia em PostgreSQL.

## Estado atual

Implementado:

- Aplicacao Spring Boot 4.1.1 em Java 25.
- Configuracao de datasource PostgreSQL por variaveis de ambiente.
- Flyway habilitado com migrations de schema versionadas.
- JPA configurado com `ddl-auto=validate`.
- Actuator com `health` e `info`.
- Autenticacao JWT Bearer inicial.
- Autorizacao por perfil em rotas administrativas, catalogo, estoque, vendas, offline, clientes e fornecedores.
- Seed de administrador por variaveis de ambiente.
- CRUD administrativo inicial de usuarios e perfis.
- CRUD inicial de categorias e produtos.
- Estoque simples com saldos por produto, entradas, ajustes e movimentacoes imutaveis.
- Clientes com CRUD inicial, busca, status e vinculo opcional na venda.
- Venda online com itens, pagamentos manuais, cliente opcional, consulta filtrada e baixa de estoque.
- Relatorio de vendas com paginacao real, totais agregados e CSV.
- Relatorio de produtos mais vendidos.
- Fornecedores com CRUD inicial e vinculo opcional em entradas de estoque.
- Sync de vendas offline com registro e resolucao manual de conflitos.
- Auditoria imutavel inicial para venda, estoque, usuarios e conflitos offline.
- `GET /actuator/health` publico.
- `GET /api/system/info` publico para validar integracao com o admin web.
- Envelope HTTP compartilhado em `api/common`.
- Tratamento global para erros de validacao e erro inesperado.
- Perfis iniciais em `UserRole`.

Ainda planejado:

- Refinamentos de UX e operacao piloto nos consumidores.
- Relatorios operacionais adicionais alem dos relatorios de vendas e produtos mais vendidos.

## Arquitetura

O backend segue fatias verticais por capacidade de negocio. Cada modulo deve evoluir com seus proprios pacotes internos:

```text
src/main/java/com/mercadoone/backend/
  api/common/          contratos HTTP compartilhados
  infrastructure/      configuracoes e adaptadores transversais
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
- `infrastructure` na raiz guarda somente configuracoes transversais.
- Modulos nao acessam detalhes internos de outros modulos.
- Integracoes entre modulos devem passar por interfaces pequenas de aplicacao, contratos HTTP, eventos ou consultas explicitamente publicadas.

## Contratos atuais

- Sucesso: `ApiEnvelope.ok(data)` retorna `success=true`, `data`, `error=null` e `timestamp`.
- Falha: `ApiEnvelope.failed(error)` retorna `success=false`, `data=null`, `error` e `timestamp`.
- Erro padronizado: `code`, `message` e `details`.
- Validacao usa codigo `VALIDATION_ERROR`.
- Erro inesperado usa codigo `INTERNAL_ERROR`.

## Comandos

```bash
mvn spring-boot:run
mvn test
```

## Documentacao local

- [Modulos do backend](docs/modulos.md)
- [Persistencia e migrations](docs/persistencia.md)

Documentacao transversal relacionada:

- [Contratos de API](../docs/api-contratos.md)
- [Dados e migracoes](../docs/dados-e-migracoes.md)
- [Seguranca](../docs/seguranca.md)
