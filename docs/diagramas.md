# Diagramas UML e Mermaid

Este documento reune diagramas versionaveis em Mermaid para explicar a arquitetura, o modelo de dominio e os fluxos criticos do Mercado One. Os diagramas refletem o estado atual do projeto: monorepo com administrativo web, API central, PDV desktop offline-first, PostgreSQL servidor e SQLite local do PDV.

Use estes diagramas como ponto de entrada antes de abrir o codigo. Para detalhes normativos, consulte tambem `README.md`, `CONTEXT.md`, `docs/arquitetura.md`, `docs/api-contratos.md`, `docs/dados-e-migracoes.md` e `docs/offline-pdv-sync.md`.

A fonte normativa e o Mermaid deste arquivo. JSON/HTML em `docs/diagramas/` sao copias visuais de verificacao e podem estar defasados (varios ainda mostram `POST /api/sales` saindo do PDV). `UML_drawio(1).xml` e sketch conceitual de estoque, nao diagrama de arquitetura atual.

## Indice

1. [Componentes do sistema](#componentes-do-sistema)
2. [Arquitetura do backend](#arquitetura-do-backend)
3. [Dominio principal](#dominio-principal)
4. [Banco servidor](#banco-servidor)
5. [Login e sessao](#login-e-sessao)
6. [Venda confirmada no servidor](#venda-confirmada-no-servidor-post-apisales)
7. [Venda no PDV](#venda-no-pdv-fila-local-e-sincronizacao)
8. [Estado da venda offline local](#estado-da-venda-offline-local)
9. [Fluxo de estoque](#fluxo-de-estoque)

## Componentes do sistema

Use este diagrama para entender os executaveis do monorepo, os bancos usados e os contratos entre Admin Web, Backend API e PDV Desktop.

```mermaid
flowchart LR
    subgraph Monorepo["Mercado One monorepo"]
        Admin["Admin Web\nAngular 22"]
        Api["Backend API\nSpring Boot 4.1 / Java 25"]
        Pdv["PDV Desktop\nJavaFX / Java 25"]
        Infra["Infra local\nDocker Compose"]
    end

    Postgres[("PostgreSQL 18\nbanco servidor")]
    Sqlite[("SQLite local\ncatalogo e fila offline")]

    Admin -->|"HTTP JSON / ApiEnvelope\ncookie HttpOnly mercado_one_admin_session"| Api
    Pdv -->|"HTTP JSON / ApiEnvelope\nAuthorization: Bearer JWT"| Api
    Api -->|"JPA + Flyway"| Postgres
    Infra -->|"sobe servico postgres"| Postgres
    Pdv -->|"preserva venda antes da rede\ncatalogo local e tentativas"| Sqlite
```

## Arquitetura do backend

Use este diagrama para localizar responsabilidades no backend. As fatias de negocio vivem em `modules/`; contratos HTTP transversais ficam em `api/common`; configuracoes compartilhadas ficam em `infrastructure`.

```mermaid
flowchart TB
    Client["Clientes HTTP\nAdmin Web e PDV Desktop"]

    subgraph Backend["backend-api"]
        Common["api/common\nApiEnvelope, ApiError,\nGlobalExceptionHandler"]
        Infra["infrastructure\nSecurityConfig,\nJwtAuthenticationFilter"]

        subgraph Modules["modules"]
            Access["access\nusuarios, autenticacao,\nperfis e JWT"]
            Catalog["catalog\ncategorias e produtos"]
            Customer["customer\nclientes e busca operacional"]
            Supplier["supplier\nfornecedores"]
            Inventory["inventory\nsaldos, entradas,\najustes e movimentacoes"]
            Sales["sales\nvendas, itens,\npagamentos e relatorios"]
            Offline["offline\nsync e conflitos\nde venda offline"]
            Audit["audit\neventos imutaveis"]
            System["system\ninfo tecnica"]
        end
    end

    Database[("PostgreSQL\nschema Flyway")]

    Client -->|"contratos REST"| Infra
    Infra -->|"autenticacao e autorizacao"| Access
    Infra --> Common
    Common -.->|"envelope e erro padrao"| Modules
    Modules -->|"JPA repositories"| Database

    Sales -->|"consulta produto ativo"| Catalog
    Sales -->|"cliente opcional ativo"| Customer
    Sales -->|"baixa de estoque"| Inventory
    Sales -->|"audita venda"| Audit
    Inventory -->|"fornecedor opcional"| Supplier
    Inventory -->|"audita movimentacao"| Audit
    Offline -->|"valida/finaliza venda offline"| Sales
    Offline -->|"audita conflito e resolucao"| Audit

    classDef boundary fill:#f8f8f8,stroke:#888,color:#222
    class Backend,Modules boundary
```

## Dominio principal

Use este diagrama para entender as entidades persistidas e enums centrais. Ele omite controllers, DTOs e campos fiscais detalhados para manter foco no dominio.

```mermaid
classDiagram
    class AppUser {
        Long id
        String name
        String login
        UserRole role
        UserStatus status
    }

    class Category {
        Long id
        String name
        boolean active
    }

    class Product {
        Long id
        String name
        String barcode
        String sku
        String unit
        BigDecimal salePrice
        boolean active
    }

    class InventoryBalance {
        Long id
        BigDecimal quantity
    }

    class InventoryMovement {
        Long id
        InventoryMovementType type
        BigDecimal quantityDelta
        BigDecimal quantityBefore
        BigDecimal quantityAfter
        String reason
    }

    class Supplier {
        Long id
        String name
        String document
        boolean active
    }

    class Customer {
        Long id
        String name
        String phone
        String document
        boolean contactConsent
        boolean active
    }

    class Sale {
        Long id
        Long operatorUserId
        SaleStatus status
        BigDecimal totalAmount
    }

    class SaleItem {
        Long id
        BigDecimal quantity
        BigDecimal unitPrice
        BigDecimal totalAmount
    }

    class SalePayment {
        Long id
        PaymentMethod method
        BigDecimal amount
    }

    class OfflineSaleConflictRecord {
        Long id
        String localSaleId
        OfflineConflictStatus status
        String conflictSummary
        String salePayload
        Long remoteSaleId
    }

    class AuditEvent {
        Long id
        String action
        String entityType
        String entityId
        Long actorUserId
        String summaryJson
    }

    class UserRole {
        <<enumeration>>
        ADMIN
        GERENTE
        OPERADOR_CAIXA
        ESTOQUISTA
    }

    class UserStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
    }

    class InventoryMovementType {
        <<enumeration>>
        ENTRY
        ADJUSTMENT
        SALE
    }

    class PaymentMethod {
        <<enumeration>>
        CASH
        CARD
        PIX
        STORE_CREDIT
    }

    class SaleStatus {
        <<enumeration>>
        CONFIRMED
    }

    class OfflineConflictStatus {
        <<enumeration>>
        PENDING
        ACCEPTED
        REJECTED
    }

    Category "1" --> "0..*" Product : organiza
    Product "1" --> "0..1" InventoryBalance : saldo
    Product "1" --> "0..*" InventoryMovement : movimenta
    Supplier "0..1" --> "0..*" InventoryMovement : origem opcional
    AppUser "0..1" --> "0..*" InventoryMovement : criou
    Customer "0..1" --> "0..*" Sale : vincula
    Sale "1" --> "1..*" SaleItem : itens
    Sale "1" --> "1..*" SalePayment : pagamentos
    Product "1" --> "0..*" SaleItem : vendido
    Sale "0..1" <-- "0..*" OfflineSaleConflictRecord : remoteSaleId
    Customer "0..1" --> "0..*" OfflineSaleConflictRecord : cliente
    AppUser "1" --> "0..*" OfflineSaleConflictRecord : operador
    AppUser "0..1" --> "0..*" AuditEvent : ator
```

## Banco servidor

Use este ERD para revisar as tabelas servidoras criadas pelas migrations Flyway de `V2` a `V7`. O SQLite local do PDV aparece em diagramas separados porque nao e fonte primaria de cadastro.

```mermaid
erDiagram
    app_users {
        bigint id PK
        varchar name
        varchar login UK
        varchar password_hash
        varchar role
        boolean active
        datetime created_at
        datetime updated_at
    }

    catalog_categories {
        bigint id PK
        varchar name UK
        boolean active
        datetime created_at
        datetime updated_at
    }

    catalog_products {
        bigint id PK
        varchar name
        varchar barcode UK
        varchar sku UK
        bigint category_id FK
        varchar unit
        numeric sale_price
        boolean active
    }

    inventory_balances {
        bigint id PK
        bigint product_id FK
        numeric quantity
        datetime created_at
        datetime updated_at
    }

    inventory_movements {
        bigint id PK
        bigint product_id FK
        varchar type
        numeric quantity_delta
        numeric quantity_before
        numeric quantity_after
        varchar reason
        bigint supplier_id FK
        bigint created_by_user_id FK
        datetime created_at
    }

    suppliers {
        bigint id PK
        varchar name
        varchar document
        varchar phone
        varchar email
        boolean active
    }

    customers {
        bigint id PK
        varchar name
        varchar phone
        varchar email
        varchar document
        boolean contact_consent
        boolean active
    }

    sales {
        bigint id PK
        bigint operator_user_id FK
        bigint customer_id FK
        varchar status
        numeric total_amount
        datetime created_at
    }

    sale_items {
        bigint id PK
        bigint sale_id FK
        bigint product_id FK
        numeric quantity
        numeric unit_price
        numeric total_amount
    }

    sale_payments {
        bigint id PK
        bigint sale_id FK
        varchar method
        numeric amount
    }

    offline_sale_conflicts {
        bigint id PK
        varchar local_sale_id
        bigint customer_id FK
        bigint operator_user_id FK
        varchar status
        text conflict_summary
        text sale_payload "JSON textual preservado"
        bigint remote_sale_id FK
        bigint resolved_by_user_id FK
    }

    audit_events {
        bigint id PK
        varchar action
        varchar entity_type
        varchar entity_id
        bigint actor_user_id FK
        text summary_json
        datetime created_at
    }

    catalog_categories ||--o{ catalog_products : categoriza
    catalog_products ||--o| inventory_balances : possui
    catalog_products ||--o{ inventory_movements : movimenta
    suppliers ||--o{ inventory_movements : fornece
    app_users ||--o{ inventory_movements : cria
    app_users ||--o{ sales : opera
    customers ||--o{ sales : compra
    sales ||--|{ sale_items : contem
    catalog_products ||--o{ sale_items : vendido_em
    sales ||--|{ sale_payments : pago_por
    customers ||--o{ offline_sale_conflicts : aparece_em
    app_users ||--o{ offline_sale_conflicts : operou
    app_users ||--o{ offline_sale_conflicts : resolveu
    sales ||--o{ offline_sale_conflicts : venda_aceita
    app_users ||--o{ audit_events : ator
```

## Login e sessao

Use este diagrama para comparar o login do Admin Web e do PDV. O mesmo endpoint autentica usuario ativo, mas o Admin Web usa cookie HttpOnly e o PDV usa token Bearer.

```mermaid
sequenceDiagram
    actor Pessoa as Administrador ou Operador
    participant Admin as Admin Web
    participant Pdv as PDV Desktop
    participant Api as Backend API
    participant Auth as AuthService
    participant Users as AppUserRepository

    alt Login pelo Admin Web
        Pessoa->>Admin: informa login e senha
        Admin->>Api: POST /api/auth/login
        Api->>Auth: autenticar credenciais
        Auth->>Users: buscar usuario ativo por login
        Users-->>Auth: AppUser
        Auth-->>Api: usuario + JWT
        Api-->>Admin: ApiEnvelope<LoginResponse> + cookie HttpOnly
        Admin->>Api: GET /api/auth/me com cookie
        Api-->>Admin: usuario atual e perfil
    else Login pelo PDV Desktop
        Pessoa->>Pdv: informa login e senha
        Pdv->>Api: POST /api/auth/login
        Api->>Auth: autenticar credenciais
        Auth->>Users: buscar usuario ativo por login
        Users-->>Auth: AppUser
        Auth-->>Api: usuario + JWT
        Api-->>Pdv: ApiEnvelope<LoginResponse>
        Pdv->>Api: chamadas protegidas com Authorization Bearer JWT
    end
```

## Venda confirmada no servidor (`POST /api/sales`)

Use este diagrama para o contrato HTTP de venda com preco vigente do servidor. A UI do PDV **nao** dispara este endpoint; o caixa atual usa o fluxo da secao seguinte. Testes de `SalesController` e `OnlineSaleClient.finalizeSale` exercitam este contrato.

```mermaid
sequenceDiagram
    actor ClienteHttp as Cliente HTTP (teste ou futuro)
    participant Api as Backend API
    participant Sales as SalesService
    participant Customers as CustomerService
    participant Products as ProductRepository
    participant Inventory as InventoryService
    participant Audit as AuditService
    participant Db as PostgreSQL

    ClienteHttp->>Api: POST /api/sales
    Api->>Sales: finalizeOnlineSale(dados, operador)

    opt cliente informado
        Sales->>Customers: getActiveCustomer(customerId)
        Customers->>Db: consultar cliente ativo
        Db-->>Customers: Customer
    end

    loop para cada item
        Sales->>Products: findById(productId)
        Products->>Db: consultar produto
        Db-->>Products: Product ativo
        Sales->>Sales: calcular item com preco vigente
    end

    Sales->>Sales: validar pagamentos iguais ao total
    Sales->>Db: salvar Sale, SaleItem e SalePayment

    loop para cada item salvo
        Sales->>Inventory: registerSaleOut(produto, quantidade)
        Inventory->>Db: bloquear saldo e gravar movimentacao SALE
    end

    Sales->>Audit: record(SALE_CONFIRMED)
    Audit->>Db: salvar AuditEvent
    Sales-->>Api: Sale CONFIRMED
    Api-->>ClienteHttp: ApiEnvelope<Sale>
```

## Venda no PDV (fila local e sincronizacao)

Use este diagrama para o fluxo real da UI JavaFX: a venda e gravada localmente antes de qualquer tentativa de rede, inclusive quando a API esta no ar. Conflitos ou falhas nao apagam o registro original. `ACCEPT`/`REJECT` no admin nao alteram o SQLite do PDV.

```mermaid
sequenceDiagram
    actor Operador as Operador de Caixa
    participant Pdv as PDV Desktop
    participant Queue as OfflineSaleQueue
    participant Sqlite as SQLite local
    participant Api as Backend API
    participant Offline as OfflineSalesService
    participant Sales as SalesService
    participant Admin as Admin Web

    Operador->>Pdv: finaliza venda presencial
    Pdv->>Queue: enqueue(LocalSale PENDING)
    Queue->>Sqlite: salvar venda, itens e pagamentos
    Queue-->>Pdv: venda preservada localmente

    Pdv->>Api: POST /api/offline/sales/sync
    Api->>Offline: syncOfflineSale(dados, operador)
    Offline->>Sales: validateOfflineSale(dados)

    alt venda aceita automaticamente
        Offline->>Sales: finalizeOfflineSale(dados, operador)
        Sales-->>Offline: Sale CONFIRMED
        Offline-->>Api: status SENT + sale
        Api-->>Pdv: ApiEnvelope<OfflineSaleSyncResult>
        Pdv->>Queue: markSent(localSaleId, remoteSaleId)
        Queue->>Sqlite: status SENT + tentativa
    else conflito de preco, produto, estoque ou pagamento
        Offline->>Sqlite: nao altera SQLite do PDV
        Offline->>Offline: salvar OfflineSaleConflictRecord
        Offline-->>Api: status CONFLICT + conflitos
        Api-->>Pdv: ApiEnvelope<OfflineSaleSyncResult>
        Pdv->>Queue: markConflict(localSaleId, mensagem)
        Queue->>Sqlite: status CONFLICT + tentativa
        Admin->>Api: GET /api/offline/sales/conflicts
        Api-->>Admin: conflitos pendentes
        Admin->>Api: POST /api/offline/sales/conflicts/{id}/resolve
        Api->>Offline: ACCEPT ou REJECT
    else falha tecnica de rede ou servidor
        Api--xPdv: erro ou indisponibilidade
        Pdv->>Queue: markError(localSaleId, mensagem)
        Queue->>Sqlite: status ERROR + tentativa
    end
```

## Estado da venda offline local

Use este diagrama para revisar o ciclo de vida persistido no SQLite do PDV. `PENDING`, `ERROR` e `CONFLICT` mantem a venda local preservada.

```mermaid
stateDiagram-v2
    [*] --> PENDING: venda gravada localmente

    PENDING --> SENT: API aceita sync
    PENDING --> CONFLICT: API registra conflito
    PENDING --> ERROR: falha tecnica

    ERROR --> SENT: reenvio no login aceito
    ERROR --> CONFLICT: reenvio no login gera conflito

    CONFLICT --> CONFLICT: venda original preservada\nadmin resolve no servidor;\nSQLite permanece CONFLICT
    SENT --> [*]: venda sincronizada

    note right of PENDING
        PDV sempre grava antes
        de tentar comunicar com a API.
    end note

    note right of CONFLICT
        Conflito nao apaga a venda.
        Admin Web aceita ou rejeita
        a pendencia no servidor.
    end note
```

## Fluxo de estoque

Use este diagrama para entender as tres origens de movimentacao de estoque no MVP: entrada, ajuste manual e baixa por venda.

```mermaid
flowchart TB
    Start([Operacao de estoque])

    Start --> Choice{Tipo de operacao}

    Choice -->|"Entrada"| Entry["Registrar entrada\nproduto ativo, quantidade,\nfornecedor opcional"]
    Choice -->|"Ajuste"| Adjustment["Registrar ajuste manual\nnova quantidade e justificativa"]
    Choice -->|"Venda confirmada"| SaleOut["Baixa por venda\nonline ou offline aceita"]

    Entry --> BalanceEntry["Localizar ou criar saldo\nInventoryBalance"]
    Adjustment --> BalanceAdjustment["Localizar ou criar saldo\nInventoryBalance"]
    SaleOut --> BalanceSale["Bloquear saldo do produto\nvalidar estoque suficiente"]

    BalanceEntry --> MovementEntry["Gravar InventoryMovement\nENTRY com fornecedor opcional"]
    BalanceAdjustment --> MovementAdjustment["Gravar InventoryMovement\nADJUSTMENT com motivo"]
    BalanceSale --> MovementSale["Gravar InventoryMovement\nSALE com delta negativo"]

    MovementEntry --> AuditEntry["Auditar INVENTORY_ENTRY"]
    MovementAdjustment --> AuditAdjustment["Auditar INVENTORY_ADJUSTMENT"]
    MovementSale --> AuditSale["Auditar INVENTORY_SALE_OUT"]

    AuditEntry --> Current["Saldo atual atualizado\ne movimentacao imutavel"]
    AuditAdjustment --> Current
    AuditSale --> Current

    Current --> Reports["Consultas e relatorios\nsaldos, movimentacoes,\nvendas e produtos mais vendidos"]
```

## Fora do escopo destes diagramas

- Casos de uso detalhados: o backlog MVP ja comunica esse escopo com mais precisao.
- Deployment cloud ou producao: a infraestrutura documentada hoje e local.
- Diagramas de telas Angular ou JavaFX: ha wireframes de planejamento em `docs/diagramas-interface-web/`; as telas reais mudam mais rapido que esses HTML.
- Modulos fora do MVP, como NFC-e, NF-e, TEF, financeiro completo, multi-loja, e-commerce ou aplicativo mobile.
