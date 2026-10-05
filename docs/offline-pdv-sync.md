# Offline PDV e Sincronização

O modo offline parcial do PDV está implementado. O sistema registra vendas localmente antes de acessar a rede, mantém um catálogo local em SQLite, sincroniza com a API e preserva conflitos para resolução administrativa.

## Objetivo

Permitir que o operador registre vendas presenciais mesmo quando a comunicação com a API estiver temporariamente indisponível, preservando a venda localmente até a sincronização.

## Estado atual

Implementado:

- `OfflineStorageConfig` define o caminho local `~/.mercado-one/pdv-offline.sqlite3`.
- Toda finalização na UI grava a venda na fila (`PENDING`) e só depois chama `POST /api/offline/sales/sync`. Isso vale também quando a API está no ar.
- Fila SQLite local com venda, itens, pagamentos e tentativas.
- Catálogo local SQLite com produtos ativos e preços vigentes, atualizado na busca vazia com sucesso, no botão de sincronizar catálogo e no login.
- Endpoints administrativos para listar e resolver conflitos de sincronização.
- No login, reenvio de vendas `PENDING` e `ERROR`.

Não implementado ainda:

- Uso da UI de `POST /api/sales` quando houver rede.
- Badge `SyncStatus` no cabeçalho (o enum existe; o label inicial `OFFLINE_READY` não muda).
- Atualização do status local após `ACCEPT`/`REJECT` no admin. A venda no PDV permanece `CONFLICT`.
- Tela no PDV para revisar a fila de conflitos.

## Estados locais persistidos

O PDV persiste os seguintes estados de venda offline (`LocalSaleStatus`):

- `PENDING`: pendente de sincronização.
- `SENT`: sincronizada e aceita pela API.
- `CONFLICT`: a API registrou conflito; a venda local é preservada.
- `ERROR`: falha técnica de sincronização; reenviada no próximo login.

O enum `SyncStatus` cobre rótulos de exibição (`ONLINE`, `OFFLINE_READY`, `SYNC_PENDING`, `SYNC_FAILED`). Não é o status persistido da venda.

No servidor, `OfflineConflictStatus` assume um dos valores `PENDING`, `ACCEPTED` e `REJECTED`. Esses valores não são escritos de volta no SQLite.

## Fluxo atual

1. Operador monta o carrinho (API ou catálogo local).
2. PDV gera `localSaleId`, grava venda/itens/pagamentos em SQLite e mostra comprovante.
3. PDV chama `POST /api/offline/sales/sync`.
4. API valida produto, preço local vs vigente, pagamento e, na confirmação, estoque.
5. Aceite automático: status local `SENT` e `remoteSaleId`.
6. Conflito: status local `CONFLICT`; pendência no admin.
7. Falha de rede/servidor: status local `ERROR`.
8. Administrador ou gerente aceita ou rejeita no admin web. O PDV não é notificado.

## Conflitos

Códigos devolvidos pela API:

- `PRODUCT_NOT_FOUND`
- `PRODUCT_INACTIVE`
- `PRICE_CHANGED`
- `PAYMENT_TOTAL`
- `STOCK_OR_PAYMENT` (estoque insuficiente ou outra regra na finalização)

`ACCEPT` registra a venda com os preços praticados pelo PDV, baixa estoque e vincula `remoteSaleId`. `REJECT` encerra a pendência no servidor. Em ambos os casos a linha SQLite permanece `CONFLICT`.

## Limites do MVP

O modo offline não permite:

- Cadastro de produtos.
- Cadastro completo de clientes.
- Cancelamento pós-venda.
- Ajuste de estoque.
- Emissão fiscal.
- Desconto.
