# Arquitetura

O Mercado One usa um monorepo com tres aplicacoes e uma area de infraestrutura local. A separacao atual reflete os canais de uso do MVP: administrativo web, API central e PDV desktop offline-first.

## Componentes

```text
admin-web -> backend-api -> PostgreSQL
pdv-desktop -> backend-api
pdv-desktop -> SQLite local
infra -> PostgreSQL local
```

- `admin-web`: interface administrativa para cadastros, estoque, CRM e relatorios.
- `backend-api`: API REST, regras servidoras, autorizacao e persistencia central.
- `pdv-desktop`: aplicacao de venda presencial, com catalogo local, fila offline e sincronizacao parcial.
- `infra`: recursos locais de desenvolvimento.

## Backend por fatias verticais

O backend e organizado por capacidade de negocio dentro de `modules/`:

- `access`: usuarios, autenticacao e perfis.
- `catalog`: produtos e categorias.
- `customer`: clientes e historico.
- `inventory`: estoque, entradas, ajustes e movimentacoes.
- `sales`: vendas, itens, pagamentos e baixa de estoque por venda online confirmada.
- `offline`: recebimento, registro de conflitos e conciliacao de vendas offline.
- `supplier`: fornecedores e vinculo opcional em entradas de estoque.
- `audit`: eventos imutaveis de operacoes criticas.
- `system`: endpoints tecnicos e informacoes do sistema.

Cada modulo pode ter `api`, `application`, `domain` e `infrastructure` internos. A criacao desses pacotes deve acompanhar a necessidade da fatia, sem antecipar implementacao vazia alem do necessario para organizar o scaffold.

## Fronteiras

- Modulos nao devem chamar classes internas de outros modulos.
- Dados compartilhados devem passar por interfaces pequenas de aplicacao, contratos HTTP, eventos ou consultas publicadas.
- `api/common` e reservado para contratos HTTP transversais.
- `infrastructure` na raiz e reservado para configuracoes e adaptadores transversais.
- Regras de dominio devem viver no modulo dono, nao em helpers globais.

## Dados

O PostgreSQL e o banco servidor. O SQLite local do PDV deve ser usado apenas para dados necessarios a operacao offline parcial, como catalogo local e fila offline.

Flyway e a fonte de verdade para mudancas no schema servidor. O Hibernate deve validar o schema, nao cria-lo automaticamente em runtime.

## Estado do scaffold

No estado atual, a arquitetura esta definida e os modulos `access`, `catalog`, `customer`, `supplier`, `inventory`, `sales`, `offline`, `audit` e `system` ja possuem casos de uso iniciais.
