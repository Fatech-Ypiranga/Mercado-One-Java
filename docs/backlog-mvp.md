# Backlog MVP

Este backlog traduz os requisitos atuais em fatias implementaveis. Itens marcados descrevem trabalho ja existente no codigo. Itens abertos descrevem lacunas reais, nao ideias novas.

## P1 - Base Operacional

- [x] Implementar autenticacao real no backend.
- [x] Persistir usuarios com nome, login/email, senha protegida, perfil e status.
- [x] Aplicar autorizacao por perfil.
- [x] Criar telas administrativas de usuarios.
- [x] Implementar cadastro de categorias.
- [x] Implementar cadastro de produtos com dados fiscais preparatorios.
- [x] Criar consulta de produtos por nome, SKU e codigo de barras.
- [x] Impedir uso de produto inativo em novas vendas.

Aceite minimo:

- Usuario inativo nao autentica.
- Operador de caixa nao acessa area administrativa sensivel (nao ha rotas de `OPERADOR_CAIXA` no admin web).
- Produto ativo fica disponivel para consulta e venda.
- Produto inativo nao aparece para nova venda.

## P2 - Estoque Simples

- [x] Persistir saldo por produto.
- [x] Registrar entrada de estoque com fornecedor opcional.
- [x] Registrar ajuste manual com justificativa.
- [x] Registrar movimentacoes imutaveis de estoque.
- [x] Exibir saldo atual por produto.
- [x] Exibir movimentacoes por produto.
- [x] Exibir movimentacoes por periodo.
- [ ] Entrada como documento com varios produtos em um unico POST (hoje `POST /api/inventory/entries` recebe um produto).

Aceite minimo do que esta entregue:

- Entrada confirmada aumenta saldo.
- Ajuste manual exige justificativa.
- Cada alteracao de saldo gera movimentacao.

## P3 - Venda no PDV

- [x] Fluxo minimo de venda no PDV com persistencia local antes da rede.
- [x] Adicionar produtos por codigo, SKU ou busca.
- [x] Alterar quantidade e remover itens antes da finalizacao.
- [x] Identificar cliente opcionalmente.
- [x] Registrar pagamento manual em dinheiro, cartao, PIX ou fiado no contrato da API.
- [x] Contrato `POST /api/sales` para venda confirmada com preco vigente do servidor.
- [x] Baixar estoque apos confirmacao no servidor.
- [x] Gerar comprovante simples sem valor fiscal.
- [ ] A UI do PDV passar a usar `POST /api/sales` quando a API estiver disponivel (hoje sempre usa fila + `POST /api/offline/sales/sync`).
- [ ] Desconto simples quando o perfil permitir (requisito conceitual, sem codigo).
- [ ] Mais de um pagamento na mesma venda na UI do PDV (a API ja aceita lista).

Aceite minimo do que esta entregue:

- Venda finalizada possui itens, operador, pagamentos, totais e horario.
- Estoque e reduzido quando o servidor aceita a venda.
- Venda aceita aparece nos relatorios basicos.

## P4 - Clientes e CRM Basico

- [x] Cadastrar cliente com nome, telefone, email, documento opcional e consentimento de contato.
- [x] Pesquisar cliente por nome, telefone ou documento.
- [x] Vincular cliente a venda.
- [x] Consultar historico de compras (`/vendas?customerId=` no admin).
- [x] Inativar cliente.

Aceite minimo:

- Cliente ativo pode ser vinculado a venda.
- Cliente inativo nao e sugerido para nova venda.
- Historico lista vendas vinculadas.

## P5 - Fornecedores e Recebimento

- [x] Cadastrar fornecedor com nome, documento, telefone, email, observacoes e status.
- [x] Vincular fornecedor opcional a entrada de estoque.
- [x] Consultar entradas por periodo e fornecedor.

Aceite minimo:

- Entrada pode ser registrada sem fornecedor.
- Entrada com fornecedor preserva o vinculo para consulta.

## P6 - Offline Parcial do PDV

- [x] Carregar catalogo local de produtos ativos e precos vigentes.
- [x] Registrar venda offline com identificador local.
- [x] Persistir venda em fila offline.
- [x] Enviar vendas pendentes quando a comunicacao retornar (login reenvia `PENDING` e `ERROR`).
- [x] Registrar conflitos de preco, produto, pagamento ou estoque.
- [x] Preservar venda offline original em conflito ou falha.
- [ ] Badge de sincronizacao no cabecalho do PDV (enum `SyncStatus` existe, mas nao e atualizado na tela).
- [ ] Refletir no PDV o aceite/rejeicao feito no admin (o status local permanece `CONFLICT`).

Aceite minimo do que esta entregue:

- Venda no PDV nao depende de comunicacao imediata para ser preservada.
- Venda pendente permanece preservada localmente.
- Venda sincronizada e aceita baixa estoque no servidor.
- Conflito nao apaga a venda original.

## P7 - Relatorios Operacionais

- [x] Vendas por periodo.
- [x] Produtos mais vendidos.
- [x] Saldo atual de estoque.
- [x] Movimentacoes de estoque por periodo.
- [x] Vendas por operador.
- [x] Vendas vinculadas a clientes.

Aceite minimo:

- Relatorios respeitam filtros basicos.
- Operador de caixa nao acessa relatorios gerenciais salvo autorizacao explicita.

## Lacunas em relacao aos requisitos conceituais

Ainda nao ha codigo para:

- Consulta administrativa de `audit_events`.
- Auditoria de criacao e alteracao de produto.
- Cancelamento pos-venda.
- Fluxo dedicado de redefinicao de senha alem do `password` opcional em `PUT /api/access/users/{id}`.

## Fora do MVP

- NFC-e, NF-e e integracao SEFAZ.
- TEF, PIX integrado e conciliacao automatica.
- Financeiro completo, contabilidade e RH.
- Multi-loja, multi-deposito, lote, validade e inventario ciclico formal.
- Fidelidade, cashback, campanhas automatizadas, e-commerce e aplicativo mobile nativo.
