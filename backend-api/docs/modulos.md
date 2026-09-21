# Modulos do Backend

O backend usa fatias verticais por capacidade de negocio. Este documento descreve cada modulo no estado atual do codigo.

## `access`

Responsavel por usuarios, perfis, autenticacao e autorizacao.

Estado atual:

- `UserRole`: `ADMIN`, `GERENTE`, `OPERADOR_CAIXA`, `ESTOQUISTA`.
- Autenticacao JWT, cookie de admin, seed de administrador e CRUD administrativo de usuarios.
- `PUT` de usuario aceita `password` opcional.

## `catalog`

Responsavel por produtos e categorias.

Estado atual:

- CRUD inicial de categorias e produtos.
- Produtos carregam categoria, unidade, preco, status e dados fiscais preparatorios (`ncm`, `cest`, `defaultCfop`, `merchandiseOrigin`, `taxClassification`).
- Busca de produto por nome, SKU ou codigo de barras.
- Nao grava evento de auditoria.

## `customer`

Responsavel por clientes.

Estado atual:

- CRUD inicial, busca administrativa e busca operacional (`GET /api/customers/search`) para o PDV.
- Preserva status, consentimento de contato e vinculo opcional com vendas.
- Historico e derivado de `GET /api/sales?customerId=`, nao de um endpoint proprio.

## `inventory`

Responsavel por estoque, entradas, ajustes e movimentacoes.

Estado atual:

- Saldo por produto.
- Entrada de um produto por `POST /api/inventory/entries`, com fornecedor opcional.
- Ajustes manuais e baixas por venda com movimentacoes imutaveis.
- Depende de `Product`/`ProductRepository` e `SupplierService`.

## `sales`

Responsavel por venda confirmada no servidor, itens, pagamentos e relatorios.

Estado atual:

- `POST /api/sales` usa preco vigente do servidor e baixa estoque.
- Consulta paginada, totais, CSV e produtos mais vendidos.
- `SaleStatus` so tem `CONFIRMED`.
- Tambem valida e finaliza payload de venda offline para o modulo `offline`.

## `offline`

Responsavel por recebimento e conciliacao de vendas enviadas pelo PDV.

Estado atual:

- `POST /api/offline/sales/sync` e o caminho da UI do PDV.
- Registra conflito quando a venda nao pode ser aceita automaticamente.
- Listagem e resolucao administrativa (`ACCEPT`/`REJECT`).
- Resolucao nao notifica o SQLite do PDV.

## `supplier`

Responsavel por fornecedores usados em entradas de estoque.

Estado atual:

- CRUD inicial.
- Vinculo real opcional em movimentacoes de entrada.

## `audit`

Responsavel por eventos imutaveis de operacoes criticas.

Estado atual:

- Grava em `audit_events`.
- Acoes observadas: `USER_CREATED`, `USER_UPDATED`, `INVENTORY_ENTRY`, `INVENTORY_ADJUSTMENT`, `INVENTORY_SALE_OUT`, `SALE_CONFIRMED`, `OFFLINE_SALE_CONFLICT`, `OFFLINE_SALE_ACCEPTED`, `OFFLINE_CONFLICT_ACCEPTED`, `OFFLINE_CONFLICT_REJECTED`.
- Sem controller de consulta.

## `system`

Responsavel por endpoints tecnicos ou informativos do sistema.

Estado atual:

- `GET /api/system/info` retorna nome da API, versao `0.1.0-SNAPSHOT` e roles.

## Regras de dependencia

- Um modulo nao deve depender de detalhes internos de outro.
- Interfaces publicadas devem ser pequenas e intencionais.
- Contratos HTTP compartilhados pertencem a `api/common`.
- Configuracoes transversais pertencem a `infrastructure`.
- O codigo ainda viola essa regra em pontos pontuais; nao amplie o acoplamento.
