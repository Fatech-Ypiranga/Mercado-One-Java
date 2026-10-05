# Arquitetura

O Mercado One usa um monorepo com três aplicações e uma área de infraestrutura local. A separação atual reflete os canais de uso do MVP: administrativo web, API central e PDV desktop offline-first.

## Componentes

```text
admin-web -> backend-api -> PostgreSQL
pdv-desktop -> backend-api
pdv-desktop -> SQLite local
infra -> PostgreSQL local
```

- `admin-web`: interface administrativa para cadastros, estoque, CRM e relatórios. Não é o canal do operador de caixa.
- `backend-api`: API REST, regras servidoras, autorização e persistência central.
- `pdv-desktop`: aplicação de venda presencial. Sempre grava a venda no SQLite local antes de sincronizar com a API.
- `infra`: PostgreSQL local de desenvolvimento.

## Backend por fatias verticais

O backend é organizado por capacidade de negócio dentro de `modules/`:

- `access`: usuários, autenticação e perfis.
- `catalog`: produtos e categorias.
- `customer`: clientes e busca operacional.
- `inventory`: estoque, entradas, ajustes e movimentações.
- `sales`: venda confirmada no servidor, itens, pagamentos e relatórios.
- `offline`: recebimento, registro de conflitos e conciliação de vendas enviadas pelo PDV.
- `supplier`: fornecedores e vínculo opcional em entradas de estoque.
- `audit`: gravação de eventos imutáveis de operações críticas.
- `system`: endpoints técnicos e informações do sistema.

Cada módulo pode ter `api`, `application`, `domain` e `infrastructure` internos. A criação desses pacotes deve acompanhar a necessidade da fatia.

Pacotes transversais:

- `api/common`: `ApiEnvelope`, `ApiError`, `GlobalExceptionHandler`.
- `infrastructure/config`: `SecurityConfig`, `JwtAuthenticationFilter`.

## Fronteiras

Regra pretendida:

- Módulos não devem chamar classes internas de outros módulos.
- Dados compartilhados devem passar por interfaces pequenas de aplicação, contratos HTTP, eventos ou consultas publicadas.
- `api/common` é reservado para contratos HTTP transversais.
- `infrastructure` na raiz é reservado para configurações e adaptadores transversais.
- Regras de domínio devem viver no módulo dono, não em helpers globais.

Acoplamento atual no código (não idealizar como alvo, apenas registrar):

- `sales` usa `ProductRepository`, `CustomerService`, `InventoryService` e `AuditService`.
- `inventory` usa entidade `Product`, `ProductRepository`, `SupplierService` e `AuditService`.
- `offline` usa `SalesService` e `AuditService`.
- `access` (gestão de usuários) usa `AuditService`.
- Entidades JPA de `sales`/`inventory` referenciam `Product`, `Customer` e `Supplier`.

## Dados

O PostgreSQL é o banco servidor. O SQLite local do PDV deve ser usado apenas para dados necessários à operação offline parcial: catálogo local e fila offline.

Flyway é a fonte de verdade para mudanças no schema servidor. O Hibernate valida o schema (`ddl-auto=validate`) e não o cria em runtime.

## Fluxo de venda no PDV

1. Operador autentica no PDV (`POST /api/auth/login`, token Bearer).
2. Busca produtos na API ou no catálogo local; busca clientes em `GET /api/customers/search`.
3. Ao finalizar, o PDV grava a venda em SQLite (`PENDING`) e exibe comprovante local.
4. Em seguida chama `POST /api/offline/sales/sync`.
5. A API aceita (`SENT`), registra conflito (`CONFLICT`) ou a chamada falha (`ERROR`).
6. `POST /api/sales` permanece disponível para venda com preço vigente do servidor; nenhum cliente da UI atual o dispara.

## Estado do scaffold

A arquitetura está definida e os módulos `access`, `catalog`, `customer`, `supplier`, `inventory`, `sales`, `offline`, `audit` e `system` já possuem casos de uso iniciais. O piloto na Azure fica em [Infraestrutura](../infra/README.md): API no App Service, PostgreSQL Flexible Server e admin no Static Web Apps. Não há fila de mensageria, worker separado nem aplicação mobile neste repositório.
