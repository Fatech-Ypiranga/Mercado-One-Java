# Modulos do Backend

O backend usa fatias verticais por capacidade de negocio. Este documento descreve a intencao de cada modulo no scaffold atual.

## `access`

Responsavel por usuarios, perfis, autenticacao e autorizacao.

Estado atual:

- Existe `UserRole` com os perfis iniciais.
- Possui autenticacao JWT, seed de administrador e CRUD administrativo de usuarios.

## `catalog`

Responsavel por produtos e categorias.

Estado atual:

- Possui CRUD inicial de categorias e produtos.
- Produtos carregam categoria, unidade, preco, status e dados fiscais preparatorios.

## `customer`

Responsavel por clientes e historico de compras.

Estado atual:

- Possui CRUD inicial de clientes, busca administrativa e busca operacional para o PDV.
- Preserva status, consentimento de contato e vinculo opcional com vendas.
- O historico e derivado de vendas vinculadas.

## `inventory`

Responsavel por estoque, entradas, ajustes e movimentacoes.

Estado atual:

- Possui saldo por produto.
- Registra entradas, ajustes manuais e baixas por venda com movimentacoes imutaveis.

## `sales`

Responsavel por vendas, itens, pagamentos e baixa de estoque por venda online confirmada.

Estado atual:

- Possui venda online minima com itens, pagamentos manuais e baixa de estoque.
- Possui consulta paginada, totais agregados, exportacao CSV e produtos mais vendidos.
- O PDV desktop e o principal consumidor operacional deste modulo.

## `offline`

Responsavel por recebimento e conciliacao de vendas offline enviadas pelo PDV.

Estado atual:

- Possui recebimento de venda offline por `POST /api/offline/sales/sync`.
- Registra conflito quando a venda nao pode ser aceita automaticamente.
- Permite listagem e resolucao administrativa de conflitos por aceite ou rejeicao.
- Deve preservar conflitos sem apagar a venda original.

## `supplier`

Responsavel por fornecedores usados em entradas de estoque.

Estado atual:

- Possui CRUD inicial de fornecedores.
- Permite vinculo real opcional em movimentacoes de entrada de estoque.

## `audit`

Responsavel por eventos imutaveis de operacoes criticas.

Estado atual:

- Registra eventos em `audit_events`.
- E usado por usuarios, estoque, vendas e conflitos offline.

## `system`

Responsavel por endpoints tecnicos ou informativos do sistema.

Estado atual:

- `GET /api/system/info` retorna nome da API, versao e roles.

## Regras de dependencia

- Um modulo nao deve depender de detalhes internos de outro.
- Interfaces publicadas devem ser pequenas e intencionais.
- Contratos HTTP compartilhados pertencem a `api/common`.
- Configuracoes transversais pertencem a `infrastructure`.
