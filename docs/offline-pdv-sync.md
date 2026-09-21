# Offline PDV e Sincronizacao

O PDV offline parcial e uma capacidade implementada. O estado atual registra vendas localmente antes da rede, mantem catalogo local SQLite, sincroniza com a API e preserva conflitos para resolucao administrativa.

## Objetivo

Permitir que o operador registre vendas presenciais mesmo quando a comunicacao com a API estiver temporariamente indisponivel, preservando a venda localmente ate a sincronizacao.

## Estado atual

Implementado:

- `OfflineStorageConfig` define o caminho local `~/.mercado-one/pdv-offline.sqlite3`.
- Toda finalizacao na UI grava a venda na fila (`PENDING`) e so depois chama `POST /api/offline/sales/sync`. Isso vale tambem quando a API esta no ar.
- Fila SQLite local com venda, itens, pagamentos e tentativas.
- Catalogo local SQLite com produtos ativos e precos vigentes, atualizado na busca vazia com sucesso, no botao de sincronizar catalogo e no login.
- Endpoints administrativos para listar e resolver conflitos de sincronizacao.
- No login, reenvio de vendas `PENDING` e `ERROR`.

Nao implementado ainda:

- Uso da UI de `POST /api/sales` quando houver rede.
- Badge `SyncStatus` no cabecalho (o enum existe; o label inicial `OFFLINE_READY` nao muda).
- Atualizacao do status local apos `ACCEPT`/`REJECT` no admin. A venda no PDV permanece `CONFLICT`.
- Tela no PDV para revisar a fila de conflitos.

## Estados locais persistidos

O PDV persiste os seguintes estados de venda offline (`LocalSaleStatus`):

- `PENDING`: pendente de sincronizacao.
- `SENT`: sincronizada e aceita pela API.
- `CONFLICT`: a API registrou conflito; a venda local e preservada.
- `ERROR`: falha tecnica de sincronizacao; reenviada no proximo login.

O enum `SyncStatus` cobre rotulos de exibicao (`ONLINE`, `OFFLINE_READY`, `SYNC_PENDING`, `SYNC_FAILED`). Nao e o status persistido da venda.

No servidor, `OfflineConflictStatus` e `PENDING`, `ACCEPTED` e `REJECTED`. Esses valores nao sao escritos de volta no SQLite.

## Fluxo atual

1. Operador monta o carrinho (API ou catalogo local).
2. PDV gera `localSaleId`, grava venda/itens/pagamentos em SQLite e mostra comprovante.
3. PDV chama `POST /api/offline/sales/sync`.
4. API valida produto, preco local vs vigente, pagamento e, na confirmacao, estoque.
5. Aceite automatico: status local `SENT` e `remoteSaleId`.
6. Conflito: status local `CONFLICT`; pendencia no admin.
7. Falha de rede/servidor: status local `ERROR`.
8. Administrador ou gerente aceita ou rejeita no admin web. O PDV nao e notificado.

## Conflitos

Codigos devolvidos pela API:

- `PRODUCT_NOT_FOUND`
- `PRODUCT_INACTIVE`
- `PRICE_CHANGED`
- `PAYMENT_TOTAL`
- `STOCK_OR_PAYMENT` (estoque insuficiente ou outra regra na finalizacao)

`ACCEPT` registra a venda com os precos praticados pelo PDV, baixa estoque e vincula `remoteSaleId`. `REJECT` encerra a pendencia no servidor. Em ambos os casos a linha SQLite permanece `CONFLICT`.

## Limites do MVP

O modo offline nao permite:

- Cadastro de produtos.
- Cadastro completo de clientes.
- Cancelamento pos-venda.
- Ajuste de estoque.
- Emissao fiscal.
- Desconto.
