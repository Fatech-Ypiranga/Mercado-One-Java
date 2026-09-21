# Arquitetura

O Mercado One usa um monorepo com tres aplicacoes e uma area de infraestrutura local. A separacao atual reflete os canais de uso do MVP: administrativo web, API central e PDV desktop offline-first.

## Componentes

```text
admin-web -> backend-api -> PostgreSQL
pdv-desktop -> backend-api
pdv-desktop -> SQLite local
infra -> PostgreSQL local
```

- `admin-web`: interface administrativa para cadastros, estoque, CRM e relatorios. Nao e o canal do operador de caixa.
- `backend-api`: API REST, regras servidoras, autorizacao e persistencia central.
- `pdv-desktop`: aplicacao de venda presencial. Sempre grava a venda no SQLite local antes de sincronizar com a API.
- `infra`: PostgreSQL local de desenvolvimento.

## Backend por fatias verticais

O backend e organizado por capacidade de negocio dentro de `modules/`:

- `access`: usuarios, autenticacao e perfis.
- `catalog`: produtos e categorias.
- `customer`: clientes e busca operacional.
- `inventory`: estoque, entradas, ajustes e movimentacoes.
- `sales`: venda confirmada no servidor, itens, pagamentos e relatorios.
- `offline`: recebimento, registro de conflitos e conciliacao de vendas enviadas pelo PDV.
- `supplier`: fornecedores e vinculo opcional em entradas de estoque.
- `audit`: gravacao de eventos imutaveis de operacoes criticas.
- `system`: endpoints tecnicos e informacoes do sistema.

Cada modulo pode ter `api`, `application`, `domain` e `infrastructure` internos. A criacao desses pacotes deve acompanhar a necessidade da fatia.

Pacotes transversais:

- `api/common`: `ApiEnvelope`, `ApiError`, `GlobalExceptionHandler`.
- `infrastructure/config`: `SecurityConfig`, `JwtAuthenticationFilter`.

## Fronteiras

Regra pretendida:

- Modulos nao devem chamar classes internas de outros modulos.
- Dados compartilhados devem passar por interfaces pequenas de aplicacao, contratos HTTP, eventos ou consultas publicadas.
- `api/common` e reservado para contratos HTTP transversais.
- `infrastructure` na raiz e reservado para configuracoes e adaptadores transversais.
- Regras de dominio devem viver no modulo dono, nao em helpers globais.

Acoplamento atual no codigo (nao idealizar como alvo, apenas registrar):

- `sales` usa `ProductRepository`, `CustomerService`, `InventoryService` e `AuditService`.
- `inventory` usa entidade `Product`, `ProductRepository`, `SupplierService` e `AuditService`.
- `offline` usa `SalesService` e `AuditService`.
- `access` (gestao de usuarios) usa `AuditService`.
- Entidades JPA de `sales`/`inventory` referenciam `Product`, `Customer` e `Supplier`.

## Dados

O PostgreSQL e o banco servidor. O SQLite local do PDV deve ser usado apenas para dados necessarios a operacao offline parcial: catalogo local e fila offline.

Flyway e a fonte de verdade para mudancas no schema servidor. O Hibernate valida o schema (`ddl-auto=validate`) e nao o cria em runtime.

## Fluxo de venda no PDV

1. Operador autentica no PDV (`POST /api/auth/login`, token Bearer).
2. Busca produtos na API ou no catalogo local; busca clientes em `GET /api/customers/search`.
3. Ao finalizar, o PDV grava a venda em SQLite (`PENDING`) e exibe comprovante local.
4. Em seguida chama `POST /api/offline/sales/sync`.
5. A API aceita (`SENT`), registra conflito (`CONFLICT`) ou a chamada falha (`ERROR`).
6. `POST /api/sales` permanece disponivel para venda com preco vigente do servidor; nenhum cliente da UI atual o dispara.

## Estado do scaffold

A arquitetura esta definida e os modulos `access`, `catalog`, `customer`, `supplier`, `inventory`, `sales`, `offline`, `audit` e `system` ja possuem casos de uso iniciais. Nao ha deploy cloud, fila de mensageria, worker separado nem aplicacao mobile neste repositorio.
