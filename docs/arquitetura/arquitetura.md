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

- Um módulo usa outro apenas pela camada `application` (services e seus records) e por entidades de `domain` em relações JPA (`Product`, `Customer`, `Supplier`).
- `api` e `infrastructure` de um módulo são internos. O teste `ModuleBoundaryTests` (ArchUnit) falha se outro módulo depender deles.
- `api/common` guarda contratos HTTP transversais; `infrastructure/config` guarda segurança e configuração.
- Regras de domínio vivem no módulo dono, não em helpers globais.

Dependências atuais:

| Módulo | Usa |
| --- | --- |
| `sales` | `CatalogService`, `CustomerService`, `InventoryService`, `AuditService` |
| `inventory` | `CatalogService`, `SupplierService`, `AuditService` |
| `offline` | `SalesService`, `CatalogService`, `InventoryService`, `AuditService` |
| `catalog`, `access` | `AuditService` |

## Dados

O PostgreSQL é o banco servidor. O SQLite local do PDV deve ser usado apenas para dados necessários à operação offline parcial: catálogo local e fila offline.

Flyway é a fonte de verdade para mudanças no schema servidor. O Hibernate valida o schema (`ddl-auto=validate`) e não o cria em runtime.

## Fluxo de venda no PDV

1. Operador autentica no PDV (`POST /api/auth/login`, token Bearer).
2. Busca produtos na API ou no catálogo local; busca clientes em `GET /api/customers/search`.
3. Ao finalizar, o PDV grava a venda em SQLite (`PENDING`) e exibe comprovante local.
4. Com sessão ativa, chama `POST /api/sales` com `clientSaleId = localSaleId` (idempotente). Sucesso marca `SENT`; falha de rede, 5xx, 401 ou 409 deixa a venda em `ERROR` para reenvio; recusa de regra de negócio marca `REJECTED`.
5. A sincronização (no login e a cada 2 minutos) reenvia `PENDING`/`ERROR` por `POST /api/offline/sales/sync` e consulta `GET /api/offline/sales/status` para refletir aceite ou rejeição de conflitos. Detalhes em [Offline PDV e sincronização](offline-pdv-sync.md).

## Escopo

O piloto na Azure fica em [Infraestrutura](../infra/README.md): API no App Service, PostgreSQL Flexible Server e admin no Static Web Apps. Não há fila de mensageria, worker separado nem aplicação mobile neste repositório.
