# Backend API

API REST do Mercado One, responsavel pelos contratos de servidor, regras centrais do MVP e persistencia em PostgreSQL.

## Estado atual

Implementado:

- Aplicacao Spring Boot 4.1.1 em Java 25, artefato `0.1.0-SNAPSHOT`.
- Configuracao de datasource PostgreSQL por `MERCADO_ONE_DATABASE_URL`, usuario e senha.
- Flyway habilitado com migrations `V1` a `V7`.
- JPA configurado com `ddl-auto=validate`.
- Actuator com `health` (publico) e `info` (autenticado).
- Autenticacao JWT Bearer e cookie HttpOnly `mercado_one_admin_session`.
- Autorizacao por perfil em rotas administrativas, catalogo, estoque, vendas, offline, clientes e fornecedores.
- Seed de administrador por variaveis de ambiente (desligado por default no YAML; ligado no `.env.example`).
- CRUD administrativo inicial de usuarios e perfis, com senha opcional na atualizacao.
- CRUD inicial de categorias e produtos, inclusive campos fiscais preparatorios.
- Estoque simples com saldo por produto, entrada unitaria, ajustes e movimentacoes imutaveis.
- Clientes com CRUD inicial, busca administrativa, busca operacional para o PDV e vinculo opcional na venda.
- `POST /api/sales` confirma venda com preco vigente do servidor, itens, pagamentos manuais e baixa de estoque. Status: so `CONFIRMED`.
- Relatorio de vendas com paginacao, totais agregados e CSV.
- Relatorio de produtos mais vendidos.
- Fornecedores com CRUD inicial e vinculo opcional em entradas de estoque.
- Sync de vendas offline com registro e resolucao manual de conflitos.
- Auditoria imutavel inicial (gravacao) para venda, estoque, usuarios e conflitos offline. Sem endpoint de leitura.
- Envelope HTTP compartilhado em `api/common`.
- Tratamento global para validacao, credencial invalida, not found, regra de negocio e erro inesperado.
- Perfis iniciais em `UserRole`.

Ainda planejado:

- Consulta de auditoria, eventos de catalogo e refinamentos de contrato para piloto.
- Relatorios operacionais adicionais alem de vendas e produtos mais vendidos.

## Arquitetura

O backend segue fatias verticais por capacidade de negocio:

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

Regras pretendidas:

- `api/common` guarda somente contratos HTTP compartilhados.
- `infrastructure` na raiz guarda somente configuracoes transversais.
- Modulos nao acessam detalhes internos de outros modulos.

No codigo atual ha acoplamento direto entre algumas fatias (`sales` -> catalog/customer/inventory/audit; `inventory` -> catalog/supplier/audit; `offline` -> sales/audit). Trate isso como estado, nao como licenca para espalhar mais.

## Contratos atuais

- Sucesso: `ApiEnvelope.ok(data)` retorna `success=true`, `data`, `error=null` e `timestamp`.
- Falha: `ApiEnvelope.failed(error)` retorna `success=false`, `data=null`, `error` e `timestamp`.
- Erro padronizado: `code`, `message` e `details`.
- Codigos: `VALIDATION_ERROR`, `BUSINESS_RULE`, `INVALID_CREDENTIALS`, `NOT_FOUND`, `INTERNAL_ERROR`.

O PDV nao chama `POST /api/sales` na UI; usa `POST /api/offline/sales/sync`.

## Comandos

```bash
mvn spring-boot:run
mvn test
```

`mvn spring-boot:run` le o `.env` da raiz do repositorio (ou do diretorio atual). A variavel obrigatoria e `MERCADO_ONE_JWT_SECRET`. Testes usam H2, `flyway.enabled=false` e `ddl-auto=create-drop`.

## Documentacao local

- [Modulos do backend](docs/modulos.md)
- [Persistencia e migrations](docs/persistencia.md)

Documentacao transversal relacionada:

- [Contratos de API](../docs/api-contratos.md)
- [Dados e migracoes](../docs/dados-e-migracoes.md)
- [Seguranca](../docs/seguranca.md)
