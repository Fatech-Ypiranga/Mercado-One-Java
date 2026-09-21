# Contratos de API

Os contratos HTTP do Mercado One devem ser consistentes entre backend, admin web e PDV desktop.

## Envelope

Todas as respostas JSON da API de negocio devem usar o envelope:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-09-07T00:00:00Z"
}
```

Em erro:

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dados invalidos.",
    "details": ["campo: mensagem"]
  },
  "timestamp": "2026-09-07T00:00:00Z"
}
```

## Campos

- `success`: indica sucesso logico da operacao.
- `data`: payload de sucesso, ou `null` em erro.
- `error`: erro padronizado, ou `null` em sucesso.
- `timestamp`: instante de geracao da resposta.

## Erros atuais

- `VALIDATION_ERROR`: erro de validacao de DTO, parametro ou constraint. HTTP 400.
- `BUSINESS_RULE`: regra de negocio violada, como login duplicado ou estoque insuficiente. HTTP 400.
- `INVALID_CREDENTIALS`: login ou senha invalidos. HTTP 401.
- `NOT_FOUND`: entidade inexistente. HTTP 404.
- `INTERNAL_ERROR`: erro inesperado sem detalhe sensivel para o cliente. HTTP 500.

## Endpoints atuais

`GET /actuator/health`

- Publico.
- Usado para health check.
- Resposta gerenciada pelo Spring Actuator.

`GET /api/system/info`

- Publico no scaffold.
- Usado pelo admin web para validar integracao.
- Retorna nome da API, versao e roles conhecidas.

Exemplo de payload:

```json
{
  "success": true,
  "data": {
    "name": "Mercado One Backend API",
    "version": "0.1.0-SNAPSHOT",
    "roles": ["ADMIN", "GERENTE", "OPERADOR_CAIXA", "ESTOQUISTA"]
  },
  "error": null,
  "timestamp": "2026-09-07T00:00:00Z"
}
```

`POST /api/auth/login`

- Publico.
- Autentica usuario ativo, retorna token JWT Bearer para clientes como o PDV e emite cookie `mercado_one_admin_session` HttpOnly para o admin web.
- Request: `{ "login": "admin", "password": "admin123" }` (login seed de `.env.example`). Testes do backend semeiam `admin@mercado.one` por propriedade, nao pelo `.env`.
- Response: `ApiEnvelope<{ accessToken, tokenType, expiresAt, user }>` com `user.id`, `user.nome`, `user.login` e `user.perfil`.
- Tambem emite cookie `mercado_one_admin_session` (HttpOnly, SameSite=Lax, Path=/). O PDV usa o `accessToken` como Bearer; o admin web usa o cookie.

`GET /api/auth/me`

- Protegido por `Authorization: Bearer <token>` ou cookie `mercado_one_admin_session`.
- Retorna `ApiEnvelope<{ id, nome, login, perfil, status }>`.

`POST /api/auth/logout`

- Exige autenticacao (nao e `permitAll`).
- Limpa o cookie HttpOnly do admin web (`maxAge=0`).
- Retorna `ApiEnvelope<null>`.

`GET /api/access/roles`

- Protegido.
- Retorna perfis tecnicos e rotulos de exibicao.
- Response: `ApiEnvelope<Array<{ value, label }>>`.

`GET /api/access/users`

- Protegido.
- Filtros opcionais: `search`, `role`, `active`.
- Retorna `ApiEnvelope<Array<{ id, nome, login, perfil, status, createdAt, updatedAt }>>`.
- Nao retorna senha nem hash de senha.

`POST /api/access/users`

- Protegido.
- Cria usuario com senha obrigatoria.
- Request: `{ "nome": "Gerente Loja", "login": "gerente", "password": "senha123", "perfil": "GERENTE", "active": true }`.

`PUT /api/access/users/{id}`

- Protegido.
- Atualiza nome, login, perfil e status.
- `password` e opcional; quando omitido ou `null`, a senha atual e preservada.

`GET /api/catalog/categories`

- Protegido.
- Filtros opcionais: `search`, `active`.

`POST /api/catalog/categories`

- Protegido.
- Request: `{ "name": "Bebidas", "active": true }`.

`PUT /api/catalog/categories/{id}`

- Protegido.
- Atualiza nome e status.

`GET /api/catalog/products`

- Protegido.
- Filtros opcionais: `search`, `categoryId`, `active`.

`POST /api/catalog/products`

- Protegido.
- Cria produto com nome, codigo de barras, SKU, categoria, unidade, preco de venda, status e dados fiscais preparatorios opcionais.
- Request: `{ "name", "barcode", "sku", "categoryId", "unit", "salePrice", "active", "ncm", "cest", "defaultCfop", "merchandiseOrigin", "taxClassification" }`.
- `search` em `GET` filtra por nome, SKU ou codigo de barras.

`PUT /api/catalog/products/{id}`

- Protegido.
- Atualiza os mesmos campos do cadastro de produto.

`GET /api/inventory/balances`

- Protegido.
- Filtros opcionais: `search`, `categoryId`, `active`.
- Retorna `ApiEnvelope<Array<{ id, product, quantity, createdAt, updatedAt }>>`.

`GET /api/inventory/movements`

- Protegido.
- Filtros opcionais: `productId`, `type`, `from`, `to`, `supplierId`.
- `type` aceita `ENTRY`, `ADJUSTMENT` e `SALE`.
- Retorna movimentacoes imutaveis com `quantityDelta`, `quantityBefore`, `quantityAfter`, motivo, fornecedor textual legado, fornecedor real opcional, documento opcional e usuario criador.

`POST /api/inventory/entries`

- Protegido.
- Registra entrada de estoque para produto ativo e aumenta o saldo.
- Request: `{ "productId": 1, "quantity": 10.000, "supplierId": 5, "supplierName": "Fornecedor", "documentNumber": "NF-1", "note": "Entrada inicial" }`.
- `supplierId` e opcional; quando informado, o fornecedor ativo real prevalece sobre `supplierName`.

`POST /api/inventory/adjustments`

- Protegido.
- Ajusta o saldo de um produto ativo para uma nova quantidade e exige justificativa.
- Request: `{ "productId": 1, "newQuantity": 8.000, "reason": "Conferencia fisica", "note": "Divergencia no saldo" }`.

`POST /api/sales`

- Protegido para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`.
- Confirma uma venda no servidor com cliente opcional, itens e pagamentos manuais.
- Usa o preco vigente do produto no servidor.
- A UI do PDV **nao** chama este endpoint; o caixa atual usa `POST /api/offline/sales/sync`. Testes e o cliente HTTP `OnlineSaleClient.finalizeSale` cobrem o contrato.
- Rejeita produto inativo, cliente inativo, estoque insuficiente e pagamento diferente do total.
- Baixa estoque apos confirmacao da venda e registra movimentacao `SALE`.
- Request:

```json
{
  "customerId": 10,
  "items": [
    { "productId": 1, "quantity": 2.000 }
  ],
  "payments": [
    { "method": "CASH", "amount": 20.00 }
  ]
}
```

- `customerId` e opcional.
- `method` aceita `CASH`, `CARD`, `PIX` e `STORE_CREDIT`.
- Response: `ApiEnvelope<{ id, operatorUserId, customer, status, totalAmount, items, payments, createdAt }>` com `status="CONFIRMED"`.
- O enum servidor `SaleStatus` so possui `CONFIRMED`. Nao ha cancelamento pos-venda.

`GET /api/sales`

- Protegido para `ADMIN` e `GERENTE`.
- Filtros opcionais: `from`, `to`, `operatorUserId`, `customerId`, `status`, `page`, `size`.
- `page` e zero-based; `size` tem padrao 50 e maximo 200.
- `from` e `to` usam `Instant` ISO-8601.
- Retorna `ApiEnvelope<{ items, page, totals }>` com vendas, metadados de pagina e totais agregados do filtro.

`GET /api/sales/export.csv`

- Protegido para `ADMIN` e `GERENTE`.
- Usa os mesmos filtros de relatorio, exceto paginacao.
- Retorna `text/csv` simples com vendas filtradas.

`GET /api/sales/top-products`

- Protegido para `ADMIN` e `GERENTE`.
- Filtros opcionais: `from`, `to`, `operatorUserId`, `customerId`, `status`, `limit`.
- `limit` tem padrao 10 e maximo 50.
- Retorna `ApiEnvelope<Array<{ productId, name, barcode, sku, unit, quantity, totalAmount }>>`.

`POST /api/offline/sales/sync`

- Protegido para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`.
- Consumidor atual da UI do PDV para toda finalizacao de venda.
- Recebe venda local do PDV com `localSaleId`, `createdAt`, cliente opcional, itens com `unitPrice` local e pagamentos.
- Retorna `status="SENT"` com venda confirmada ou `status="CONFLICT"` com lista de conflitos.
- Codigos de conflito observados no codigo: `PRODUCT_NOT_FOUND`, `PRODUCT_INACTIVE`, `PRICE_CHANGED`, `PAYMENT_TOTAL`, `STOCK_OR_PAYMENT`.
- Quando ha conflito, registra uma pendencia administrativa consultavel. Aceite/rejeicao no admin nao possui callback para atualizar o SQLite do PDV.

`GET /api/offline/sales/conflicts`

- Protegido para `ADMIN` e `GERENTE`.
- Filtro opcional: `status`, com padrao `PENDING`; aceita `PENDING`, `ACCEPTED` e `REJECTED`.
- Retorna `ApiEnvelope<Array<{ id, localSaleId, createdAt, customerId, operatorUserId, status, conflictSummary, salePayload, remoteSaleId, resolutionNote, resolvedByUserId, resolvedAt, updatedAt }>>`.

`POST /api/offline/sales/conflicts/{id}/resolve`

- Protegido para `ADMIN` e `GERENTE`.
- Resolve conflito offline pendente.
- Request: `{ "action": "ACCEPT", "note": "Preco praticado validado" }`.
- `action` aceita `ACCEPT` e `REJECT`.
- `note` e obrigatoria quando `action="REJECT"` para registrar o motivo operacional da recusa.
- `ACCEPT` registra a venda com os precos praticados pelo PDV, baixa estoque e vincula `remoteSaleId`.
- `REJECT` apenas encerra a pendencia preservando a auditoria.

`GET /api/suppliers`

- Protegido para `ADMIN` e `GERENTE`.
- Filtros opcionais: `search`, `active`.

`GET /api/suppliers/{id}`

- Protegido para `ADMIN` e `GERENTE`.
- Retorna fornecedor pelo identificador.

`POST /api/suppliers`

- Protegido para `ADMIN` e `GERENTE`.
- Cria fornecedor com nome, documento, telefone, email, observacoes e status.

`PUT /api/suppliers/{id}`

- Protegido para `ADMIN` e `GERENTE`.
- Atualiza os mesmos campos do cadastro de fornecedor.

`GET /api/customers`

- Leitura protegida para `ADMIN` e `GERENTE`.
- Filtros opcionais: `search`, `active`.
- Pesquisa por nome, telefone, e-mail ou documento.
- Retorna `ApiEnvelope<Array<{ id, name, phone, email, document, contactConsent, active, createdAt, updatedAt }>>`.

`GET /api/customers/{id}`

- Leitura protegida para `ADMIN` e `GERENTE`.
- Retorna cliente pelo identificador.
- Nao ha endpoint dedicado de historico: use `GET /api/sales?customerId=`.

`GET /api/customers/search`

- Busca operacional protegida para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`.
- Retorna somente clientes ativos e campos resumidos: `id`, `name`, `phone`, `document`.
- Usado pelo PDV para identificacao opcional sem expor a ficha administrativa completa.

`POST /api/customers`

- Protegido para `ADMIN` e `GERENTE`.
- Cria cliente.
- Request: `{ "name": "Maria Silva", "phone": "11999990000", "email": "maria@example.com", "document": "12345678900", "contactConsent": true, "active": true }`.

`PUT /api/customers/{id}`

- Protegido para `ADMIN` e `GERENTE`.
- Atualiza os mesmos campos do cadastro de cliente.

## Autorizacao atual

- `/api/access/users/**`: `ADMIN`.
- `GET /api/catalog/**`: `ADMIN`, `GERENTE`, `ESTOQUISTA`, `OPERADOR_CAIXA`.
- Escrita em `/api/catalog/**`: `ADMIN`, `GERENTE`.
- `/api/inventory/**`: `ADMIN`, `GERENTE`, `ESTOQUISTA`.
- `POST /api/sales/**`: `ADMIN`, `GERENTE`, `OPERADOR_CAIXA`.
- `GET /api/sales/**`: `ADMIN`, `GERENTE`.
- `/api/offline/sales/conflicts/**`: `ADMIN`, `GERENTE`.
- Demais `/api/offline/**`: `ADMIN`, `GERENTE`, `OPERADOR_CAIXA`.
- `/api/suppliers/**`: `ADMIN`, `GERENTE`.
- `GET /api/customers/search`: `ADMIN`, `GERENTE`, `OPERADOR_CAIXA`.
- `GET /api/customers/**`: `ADMIN`, `GERENTE`.
- Escrita em `/api/customers/**`: `ADMIN`, `GERENTE`.
- `/api/auth/me` e `/api/access/roles`: qualquer usuario autenticado.
- `POST /api/auth/logout`: qualquer usuario autenticado.

Nao existem neste repositorio: endpoints de consulta de `audit_events`, `DELETE`/`PATCH` de negocio, emissao fiscal, desconto ou cancelamento de venda.

## Convencoes futuras

- Endpoints de negocio devem ficar dentro do modulo dono.
- DTOs publicos nao devem expor entidades JPA diretamente.
- Validacao deve ocorrer na borda HTTP e nas regras de dominio relevantes.
- Mensagens de erro para usuario devem ser claras e nao expor detalhes internos.
- Mudancas incompatíveis em contrato devem ser registradas antes da implementacao.
