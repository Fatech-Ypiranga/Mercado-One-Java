# Contratos de API

Os contratos HTTP do Mercado One devem ser consistentes entre backend, admin web e PDV desktop.

## Envelope

Todas as respostas JSON da API de negócio devem usar o envelope:

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

- `success`: indica sucesso lógico da operação.
- `data`: payload de sucesso, ou `null` em erro.
- `error`: erro padronizado, ou `null` em sucesso.
- `timestamp`: instante de geração da resposta.

## Erros atuais

- `VALIDATION_ERROR`: erro de validação de DTO, parâmetro ou constraint. HTTP 400.
- `BUSINESS_RULE`: regra de negócio violada, como login duplicado ou estoque insuficiente. HTTP 400.
- `MALFORMED_REQUEST`: corpo ou parâmetro em formato inválido. HTTP 400.
- `INVALID_CREDENTIALS`: login ou senha inválidos. HTTP 401.
- `NOT_FOUND`: entidade ou rota inexistente. HTTP 404.
- `DATA_CONFLICT`: valor duplicado ou referência inválida no banco. HTTP 409.
- `INTERNAL_ERROR`: erro inesperado sem detalhe sensível para o cliente. HTTP 500.

## Endpoints atuais

`GET /actuator/health`

- Público.
- Usado para health check.
- Resposta gerenciada pelo Spring Actuator.

`GET /api/system/info`

- Público no scaffold.
- Usado pelo admin web para validar integração.
- Retorna nome da API, versão e roles conhecidas.

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

- Público.
- Autentica usuário ativo, retorna token JWT Bearer para clientes como o PDV e emite cookie `mercado_one_admin_session` HttpOnly para o admin web.
- Request: `{ "login": "admin", "password": "admin123" }` (login seed de `.env.example`). Testes do backend semeiam `admin@mercado.one` por propriedade, não pelo `.env`.
- Response: `ApiEnvelope<{ accessToken, tokenType, expiresAt, user }>` com `user.id`, `user.nome`, `user.login` e `user.perfil`.
- Também emite cookie `mercado_one_admin_session` (HttpOnly, Path=/, SameSite=Lax no ambiente local ou Secure e SameSite=None com `MERCADO_ONE_COOKIE_SECURE=true`). O PDV usa o `accessToken` como Bearer; o admin web usa o cookie.

`GET /api/auth/me`

- Protegido por `Authorization: Bearer <token>` ou cookie `mercado_one_admin_session`.
- Retorna `ApiEnvelope<{ id, nome, login, perfil, status }>`.

`POST /api/auth/logout`

- Exige autenticação (não é `permitAll`).
- Limpa o cookie HttpOnly do admin web (`maxAge=0`). Com `MERCADO_ONE_COOKIE_SECURE=true`, exige `Origin` do admin permitido ou da própria API.
- Retorna `ApiEnvelope<null>`.

`POST /api/auth/password`

- Qualquer usuário autenticado troca a própria senha.
- Request: `{ "currentPassword": "...", "newPassword": "..." }`. Nova senha com 6 a 120 caracteres.
- Tokens emitidos antes da troca deixam de valer.

`GET /api/access/roles`

- Protegido.
- Retorna perfis técnicos e rótulos de exibição.
- Response: `ApiEnvelope<Array<{ value, label }>>`.

`GET /api/access/users`

- Protegido.
- Filtros opcionais: `search`, `role`, `active`.
- Retorna `ApiEnvelope<Array<{ id, nome, login, perfil, status, createdAt, updatedAt }>>`.
- Não retorna senha nem hash de senha.

`POST /api/access/users`

- Protegido.
- Cria usuário com senha obrigatória.
- Request: `{ "nome": "Gerente Loja", "login": "gerente", "password": "senha123", "perfil": "GERENTE", "active": true }`.

`PUT /api/access/users/{id}`

- Protegido.
- Atualiza nome, login, perfil e status.
- `password` é opcional; quando omitido ou `null`, a senha atual é preservada.

`POST /api/access/users/{id}/password`

- Protegido para `ADMIN`.
- Redefinição administrativa de senha. Request: `{ "password": "..." }`, com 6 a 120 caracteres.

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
- Cria produto com nome, código de barras, SKU, categoria, unidade, preço de venda, status e dados fiscais preparatórios opcionais.
- Request: `{ "name", "barcode", "sku", "categoryId", "unit", "salePrice", "active", "ncm", "cest", "defaultCfop", "merchandiseOrigin", "taxClassification" }`.
- `search` em `GET` filtra por nome, SKU ou código de barras.

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
- `type` aceita `ENTRY`, `ADJUSTMENT`, `SALE` e `SALE_CANCELLATION`.
- Retorna movimentações imutáveis com `quantityDelta`, `quantityBefore`, `quantityAfter`, motivo, fornecedor textual legado, fornecedor real opcional, documento opcional e usuário criador.

`POST /api/inventory/entries`

- Protegido.
- Registra entrada de estoque para produto ativo e aumenta o saldo.
- Request: `{ "productId": 1, "quantity": 10.000, "supplierId": 5, "supplierName": "Fornecedor", "documentNumber": "NF-1", "note": "Entrada inicial" }`.
- `supplierId` é opcional; quando informado, o fornecedor ativo real prevalece sobre `supplierName`.

`POST /api/inventory/entry-documents`

- Protegido.
- Nota de entrada com vários produtos numa única operação. Gera uma movimentação `ENTRY` por item.
- Request: `{ "supplierId": 5, "supplierName": null, "documentNumber": "NF-1", "note": "...", "items": [ { "productId": 1, "quantity": 10.000 } ] }`.
- `items` exige de 1 a 200 itens. Retorna a lista de movimentações criadas.

`POST /api/inventory/adjustments`

- Protegido.
- Ajusta o saldo de um produto ativo para uma nova quantidade e exige justificativa.
- Request: `{ "productId": 1, "newQuantity": 8.000, "reason": "Conferência física", "note": "Divergência no saldo" }`.

`POST /api/sales`

- Protegido para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`.
- Confirma uma venda no servidor com cliente opcional, itens e pagamentos manuais.
- Usa o preço vigente do produto no servidor.
- Usado pelo PDV na finalização com API disponível. Idempotente por `clientSaleId`: reenvio devolve a venda existente.
- Rejeita produto inativo, cliente inativo, estoque insuficiente, desconto acima do limite do perfil e pagamentos com soma diferente do total líquido.
- Baixa estoque após confirmação da venda e registra movimentação `SALE`.
- Request:

```json
{
  "customerId": 10,
  "items": [
    { "productId": 1, "quantity": 2.000 }
  ],
  "payments": [
    { "method": "CASH", "amount": 15.00 },
    { "method": "PIX", "amount": 3.00 }
  ],
  "discountAmount": 2.00,
  "clientSaleId": "pdv-0001"
}
```

- `customerId`, `discountAmount` e `clientSaleId` são opcionais. Aceita mais de um pagamento.
- `method` aceita `CASH`, `CARD`, `PIX` e `STORE_CREDIT`.
- Response: `ApiEnvelope<{ id, operatorUserId, customer, status, grossAmount, discountAmount, totalAmount, items, payments, createdAt, clientSaleId, canceledAt, canceledByUserId, cancelReason }>` com `status="CONFIRMED"`.
- `SaleStatus` tem `CONFIRMED` e `CANCELED`.

`POST /api/sales/{id}/cancel`

- Protegido para `ADMIN` e `GERENTE`.
- Cancela venda confirmada. Request: `{ "reason": "Cliente desistiu" }`, motivo obrigatório (até 500 caracteres).
- Devolve o estoque de cada item com movimentação `SALE_CANCELLATION` e audita `SALE_CANCELED`.

`GET /api/sales/discount-policy`

- Protegido para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`.
- Retorna `{ role, maxDiscountPercent }`. `null` significa sem limite além do total bruto (`ADMIN`, `GERENTE`). Para `OPERADOR_CAIXA` vale `mercado-one.sales.operator-max-discount-percent` (padrão 0, sem desconto).

`GET /api/sales/mine`

- Protegido para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`.
- Mesmo formato de `GET /api/sales`, filtrado pelo usuário autenticado. Filtros: `from`, `to`, `status`, `page`, `size`. Usado pela tela "Meu caixa".

`GET /api/sales`

- Protegido para `ADMIN` e `GERENTE`.
- Filtros opcionais: `from`, `to`, `operatorUserId`, `customerId`, `status`, `page`, `size`.
- `page` é zero-based; `size` tem padrão 50 e máximo 200.
- `from` e `to` usam `Instant` ISO-8601.
- Retorna `ApiEnvelope<{ items, page, totals }>` com vendas, metadados de página e totais agregados do filtro.

`GET /api/sales/export.csv`

- Protegido para `ADMIN` e `GERENTE`.
- Usa os mesmos filtros de relatório, exceto paginação.
- Retorna `text/csv` simples com vendas filtradas.

`GET /api/sales/top-products`

- Protegido para `ADMIN` e `GERENTE`.
- Filtros opcionais: `from`, `to`, `operatorUserId`, `customerId`, `status`, `limit`.
- `limit` tem padrão 10 e máximo 50.
- Retorna `ApiEnvelope<Array<{ productId, name, barcode, sku, unit, quantity, totalAmount }>>`.

`POST /api/offline/sales/sync`

- Protegido para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`.
- Usado pelo PDV para reenviar vendas `PENDING` e `ERROR` da fila local.
- Recebe venda local do PDV com `localSaleId`, `createdAt`, cliente opcional, itens com `unitPrice` local, pagamentos e `discountAmount` opcional.
- Idempotente: se já existe venda com `clientSaleId` igual ao `localSaleId`, devolve `SENT` sem gravar de novo.
- Retorna `status="SENT"` com venda confirmada ou `status="CONFLICT"` com lista de conflitos.
- Códigos de conflito observados no código: `PRODUCT_NOT_FOUND`, `PRODUCT_INACTIVE`, `PRICE_CHANGED`, `PAYMENT_TOTAL`, `STOCK_INSUFFICIENT`, `DISCOUNT_INVALID`, `DISCOUNT_NOT_ALLOWED`.
- Quando há conflito, registra uma pendência administrativa consultável. O PDV descobre a decisão do admin por `GET /api/offline/sales/status`.

`GET /api/offline/sales/status`

- Protegido para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`.
- Parâmetro `localSaleIds` (lista, no máximo 200).
- Retorna `ApiEnvelope<Array<{ localSaleId, status, remoteSaleId, note }>>`. O PDV aplica `SENT` ou `REJECTED` às vendas locais em `CONFLICT`.

`GET /api/offline/sales/conflicts`

- Protegido para `ADMIN` e `GERENTE`.
- Filtro opcional: `status`, com padrão `PENDING`; aceita `PENDING`, `ACCEPTED` e `REJECTED`.
- Retorna `ApiEnvelope<Array<{ id, localSaleId, createdAt, customerId, operatorUserId, status, conflictSummary, salePayload, remoteSaleId, resolutionNote, resolvedByUserId, resolvedAt, updatedAt }>>`.

`POST /api/offline/sales/conflicts/{id}/resolve`

- Protegido para `ADMIN` e `GERENTE`.
- Resolve conflito offline pendente.
- Request: `{ "action": "ACCEPT", "note": "Preço praticado validado" }`.
- `action` aceita `ACCEPT` e `REJECT`.
- `note` é obrigatória quando `action="REJECT"` para registrar o motivo operacional da recusa.
- `ACCEPT` registra a venda com os preços, desconto, data e operador originais do PDV, baixa estoque (o saldo pode ficar negativo, pois a mercadoria já saiu) e vincula `remoteSaleId`.
- `REJECT` apenas encerra a pendência preservando a auditoria.

`GET /api/audit/events`

- Protegido para `ADMIN`.
- Filtros opcionais: `action`, `entityType`, `entityId`, `actorUserId`, `from`, `to`, `page`, `size`.
- Ordem decrescente por data. Retorna `ApiEnvelope<{ items, page, size, totalElements, totalPages }>` com `{ id, action, entityType, entityId, actorUserId, summaryJson, createdAt }`.

`GET /api/audit/actions`

- Protegido para `ADMIN`.
- Lista as ações já registradas, para o filtro da tela de auditoria.

`GET /api/suppliers`

- Protegido para `ADMIN` e `GERENTE`.
- Filtros opcionais: `search`, `active`.

`GET /api/suppliers/{id}`

- Protegido para `ADMIN` e `GERENTE`.
- Retorna fornecedor pelo identificador.

`POST /api/suppliers`

- Protegido para `ADMIN` e `GERENTE`.
- Cria fornecedor com nome, documento, telefone, e-mail, observações e status.

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
- Não há endpoint dedicado de histórico: use `GET /api/sales?customerId=`.

`GET /api/customers/search`

- Busca operacional protegida para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`.
- Retorna somente clientes ativos e campos resumidos: `id`, `name`, `phone`, `document`.
- Usado pelo PDV para identificação opcional sem expor a ficha administrativa completa.

`POST /api/customers/quick`

- Protegido para `ADMIN`, `GERENTE` e `OPERADOR_CAIXA`.
- Cadastro rápido no caixa. Request: `{ "name": "Maria", "phone": "11999990000", "document": null, "contactConsent": true }`. Cria cliente ativo e retorna o resumo da busca operacional.

`POST /api/customers`

- Protegido para `ADMIN` e `GERENTE`.
- Cria cliente.
- Request: `{ "name": "Maria Silva", "phone": "11999990000", "email": "maria@example.com", "document": "12345678900", "contactConsent": true, "active": true }`.

`PUT /api/customers/{id}`

- Protegido para `ADMIN` e `GERENTE`.
- Atualiza os mesmos campos do cadastro de cliente.

## Autorização atual

- `/api/access/users/**`: `ADMIN`.
- `GET /api/catalog/**`: `ADMIN`, `GERENTE`, `ESTOQUISTA`, `OPERADOR_CAIXA`.
- Escrita em `/api/catalog/**`: `ADMIN`, `GERENTE`.
- `/api/inventory/**`: `ADMIN`, `GERENTE`, `ESTOQUISTA`.
- `/api/audit/**`: `ADMIN`.
- `POST /api/sales/{id}/cancel`: `ADMIN`, `GERENTE`.
- `GET /api/sales/mine` e `GET /api/sales/discount-policy`: `ADMIN`, `GERENTE`, `OPERADOR_CAIXA`.
- Demais `POST /api/sales/**`: `ADMIN`, `GERENTE`, `OPERADOR_CAIXA`.
- Demais `GET /api/sales/**`: `ADMIN`, `GERENTE`.
- `/api/offline/sales/conflicts/**`: `ADMIN`, `GERENTE`.
- Demais `/api/offline/**`: `ADMIN`, `GERENTE`, `OPERADOR_CAIXA`.
- `/api/suppliers/**`: `ADMIN`, `GERENTE`.
- `POST /api/customers/quick` e `GET /api/customers/search`: `ADMIN`, `GERENTE`, `OPERADOR_CAIXA`.
- Demais `/api/customers/**`: `ADMIN`, `GERENTE`.
- `/api/auth/me`, `POST /api/auth/password`, `POST /api/auth/logout` e `/api/access/roles`: qualquer usuário autenticado.

Escrita autenticada por cookie (métodos diferentes de `GET`, `HEAD`, `OPTIONS` e `TRACE`) exige `Origin` ou `Referer` de origem confiável; detalhes em [Segurança](seguranca.md).

Não existem neste repositório: `DELETE`/`PATCH` de negócio e emissão fiscal.

## Convenções futuras

- Endpoints de negócio devem ficar dentro do módulo dono.
- DTOs públicos não devem expor entidades JPA diretamente.
- Validação deve ocorrer na borda HTTP e nas regras de domínio relevantes.
- Mensagens de erro para usuário devem ser claras e não expor detalhes internos.
- Mudanças incompatíveis em contrato devem ser registradas antes da implementação.
