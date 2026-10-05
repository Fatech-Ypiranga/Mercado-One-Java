# Backlog MVP

Este backlog traduz os requisitos atuais em fatias implementáveis. Itens marcados descrevem trabalho já existente no código. Itens abertos descrevem lacunas reais, não ideias novas.

## P1 - Base Operacional

- [x] Implementar autenticação real no backend.
- [x] Persistir usuários com nome, login/email, senha protegida, perfil e status.
- [x] Aplicar autorização por perfil.
- [x] Criar telas administrativas de usuários.
- [x] Implementar cadastro de categorias.
- [x] Implementar cadastro de produtos com dados fiscais preparatórios.
- [x] Criar consulta de produtos por nome, SKU e código de barras.
- [x] Impedir uso de produto inativo em novas vendas.

Aceite mínimo:

- Usuário inativo não autentica.
- Operador de caixa não acessa área administrativa sensível (não há rotas de `OPERADOR_CAIXA` no admin web).
- Produto ativo fica disponível para consulta e venda.
- Produto inativo não aparece para nova venda.

## P2 - Estoque Simples

- [x] Persistir saldo por produto.
- [x] Registrar entrada de estoque com fornecedor opcional.
- [x] Registrar ajuste manual com justificativa.
- [x] Registrar movimentações imutáveis de estoque.
- [x] Exibir saldo atual por produto.
- [x] Exibir movimentações por produto.
- [x] Exibir movimentações por período.
- [ ] Entrada como documento com vários produtos em um único POST (hoje `POST /api/inventory/entries` recebe um produto).

Aceite mínimo do que está entregue:

- Entrada confirmada aumenta saldo.
- Ajuste manual exige justificativa.
- Cada alteração de saldo gera movimentação.

## P3 - Venda no PDV

- [x] Fluxo mínimo de venda no PDV com persistência local antes da rede.
- [x] Adicionar produtos por código, SKU ou busca.
- [x] Alterar quantidade e remover itens antes da finalização.
- [x] Identificar cliente opcionalmente.
- [x] Registrar pagamento manual em dinheiro, cartão, PIX ou fiado no contrato da API.
- [x] Contrato `POST /api/sales` para venda confirmada com preço vigente do servidor.
- [x] Baixar estoque após confirmação no servidor.
- [x] Gerar comprovante simples sem valor fiscal.
- [ ] A UI do PDV passar a usar `POST /api/sales` quando a API estiver disponível (hoje sempre usa fila + `POST /api/offline/sales/sync`).
- [ ] Desconto simples quando o perfil permitir (requisito conceitual, sem código).
- [ ] Mais de um pagamento na mesma venda na UI do PDV (a API já aceita lista).

Aceite mínimo do que está entregue:

- Venda finalizada possui itens, operador, pagamentos, totais e horário.
- Estoque é reduzido quando o servidor aceita a venda.
- Venda aceita aparece nos relatórios básicos.

## P4 - Clientes e CRM Básico

- [x] Cadastrar cliente com nome, telefone, email, documento opcional e consentimento de contato.
- [x] Pesquisar cliente por nome, telefone ou documento.
- [x] Vincular cliente a uma venda.
- [x] Consultar histórico de compras (`/vendas?customerId=` no admin).
- [x] Inativar cliente.

Aceite mínimo:

- Cliente ativo pode ser vinculado a uma venda.
- Cliente inativo não é sugerido para nova venda.
- Histórico lista vendas vinculadas.

## P5 - Fornecedores e Recebimento

- [x] Cadastrar fornecedor com nome, documento, telefone, email, observações e status.
- [x] Vincular fornecedor opcional a uma entrada de estoque.
- [x] Consultar entradas por período e fornecedor.

Aceite mínimo:

- Entrada pode ser registrada sem fornecedor.
- Entrada com fornecedor preserva o vínculo para consulta.

## P6 - Offline Parcial do PDV

- [x] Carregar catálogo local de produtos ativos e preços vigentes.
- [x] Registrar venda offline com identificador local.
- [x] Persistir venda em fila offline.
- [x] Enviar vendas pendentes quando a comunicação retornar (login reenvia `PENDING` e `ERROR`).
- [x] Registrar conflitos de preço, produto, pagamento ou estoque.
- [x] Preservar venda offline original em conflito ou falha.
- [ ] Badge de sincronização no cabeçalho do PDV (enum `SyncStatus` existe, mas não é atualizado na tela).
- [ ] Refletir no PDV o aceite/rejeição feito no admin (o status local permanece `CONFLICT`).

Aceite mínimo do que está entregue:

- Venda no PDV não depende de comunicação imediata para ser preservada.
- Venda pendente permanece preservada localmente.
- A venda sincronizada e aceita pelo servidor reduz o estoque.
- Conflito não apaga a venda original.

## P7 - Relatórios Operacionais

- [x] Vendas por período.
- [x] Produtos mais vendidos.
- [x] Saldo atual de estoque.
- [x] Movimentações de estoque por período.
- [x] Vendas por operador.
- [x] Vendas vinculadas a clientes.

Aceite mínimo:

- Relatórios respeitam filtros básicos.
- Operador de caixa não acessa relatórios gerenciais salvo autorização explícita.

## Lacunas em relação aos requisitos conceituais

Ainda não há código para:

- Consulta administrativa de `audit_events`.
- Auditoria de criação e alteração de produto.
- Cancelamento pós-venda.
- Fluxo dedicado de redefinição de senha além do `password` opcional em `PUT /api/access/users/{id}`.

## Fora do MVP

- NFC-e, NF-e e integração SEFAZ.
- TEF, PIX integrado e conciliação automática.
- Financeiro completo, contabilidade e RH.
- Multi-loja, multi-depósito, lote, validade e inventário cíclico formal.
- Fidelidade, cashback, campanhas automatizadas, e-commerce e aplicativo mobile nativo.
