# Offline PDV e Sincronização

O PDV registra toda venda no SQLite local antes de acessar a rede. A venda local é a fonte de verdade até o servidor confirmá-la.

## Armazenamento local

- Caminho padrão: `~/.mercado-one/pdv-offline.sqlite3` (`OfflineStorageConfig`).
- Fila: `offline_sales`, `offline_sale_items`, `offline_sale_payments` e `offline_sale_sync_attempts` (histórico de tentativas).
- Catálogo: `local_catalog_products`, substituído por completo no login, na busca vazia bem-sucedida e no botão de atualizar catálogo. Usado quando a API não responde ou não há sessão.

## Estados da venda local (`LocalSaleStatus`)

| Estado | Significado | Reenviada? |
| --- | --- | --- |
| `PENDING` | Gravada, ainda não enviada | Sim |
| `ERROR` | Falha técnica (rede, 5xx, sessão expirada, 409) | Sim |
| `SENT` | Confirmada no servidor; guarda `remote_sale_id` | Não |
| `CONFLICT` | Servidor registrou conflito; aguarda decisão no admin | Não; status consultado |
| `REJECTED` | Recusada (regra de negócio no envio online ou conflito rejeitado no admin) | Não |

`SyncStatus` (`ONLINE`, `OFFLINE_READY`, `SYNC_PENDING`, `SYNC_FAILED`) é o indicador exibido no PDV, calculado a partir da fila e da última resposta da API.

## Fluxo

1. Finalização grava a venda como `PENDING` e mostra o comprovante não fiscal.
2. Com sessão, o PDV chama `POST /api/sales` com `clientSaleId = localSaleId`. Preço é o vigente no servidor.
3. A sincronização roda no login, a cada 2 minutos e pelo botão da fila. Uma execução por vez.
   - Reenvia `PENDING`/`ERROR` em `POST /api/offline/sales/sync`, com os preços praticados no PDV e o `createdAt` local.
   - Falha de rede interrompe a rodada; 5xx marca só aquela venda como `ERROR` e segue para a próxima.
   - Consulta `GET /api/offline/sales/status` para vendas em `CONFLICT` e aplica `SENT` ou `REJECTED`.

## Servidor

`POST /api/offline/sales/sync`:

1. Se já existe venda com o mesmo `clientSaleId`, devolve `SENT` (idempotência; índice único `ux_sales_client_sale_id`).
2. Valida sem gravar: produto existe e está ativo, preço igual ao vigente, estoque disponível, desconto válido e dentro do limite do perfil, pagamento igual ao total.
3. Com conflito, grava ou atualiza `offline_sale_conflicts` (`PENDING`) e devolve `CONFLICT`.
4. Sem conflito, confirma a venda com a data local (`createdAt`) e fecha conflito pendente anterior da mesma venda.

Códigos de conflito: `PRODUCT_NOT_FOUND`, `PRODUCT_INACTIVE`, `PRICE_CHANGED`, `STOCK_INSUFFICIENT`, `DISCOUNT_INVALID`, `DISCOUNT_NOT_ALLOWED`, `PAYMENT_TOTAL`.

## Resolução no admin

`POST /api/offline/sales/conflicts/{id}/resolve` (ADMIN, GERENTE):

- `ACCEPT`: confirma a venda com preços, desconto, data e operador originais. Como a mercadoria já foi entregue, o estoque pode ficar negativo. Produto inativo ou inexistente impede o aceite.
- `REJECT`: exige observação. A venda local passa a `REJECTED` na próxima sincronização.

## Limites

Offline não permite cadastro de produto, ajuste de estoque, cancelamento pós-venda nem emissão fiscal.
