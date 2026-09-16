# Backlog MVP

Este backlog traduz os requisitos atuais em fatias implementaveis. Ele descreve trabalho planejado, nao funcionalidade ja existente.

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
- Operador de caixa nao acessa area administrativa sensivel.
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

Aceite minimo:

- Entrada confirmada aumenta saldo.
- Ajuste manual exige justificativa.
- Cada alteracao de saldo gera movimentacao.

## P3 - Venda Online

- [x] Criar fluxo minimo de venda online no PDV.
- [x] Adicionar produtos por codigo, SKU ou busca.
- [x] Alterar quantidade e remover itens antes da finalizacao.
- [x] Identificar cliente opcionalmente.
- [x] Registrar pagamento manual em dinheiro, cartao, PIX ou fiado no contrato da API.
- [x] Finalizar venda online minima.
- [x] Baixar estoque apos confirmacao.
- [x] Gerar comprovante simples sem valor fiscal.

Aceite minimo:

- Venda finalizada possui itens, operador, pagamentos, totais e horario.
- Estoque e reduzido para os produtos vendidos.
- Venda aparece nos relatorios basicos.

## P4 - Clientes e CRM Basico

- [x] Cadastrar cliente com nome, telefone, email, documento opcional e consentimento de contato.
- [x] Pesquisar cliente por nome, telefone ou documento.
- [x] Vincular cliente a venda.
- [x] Consultar historico de compras.
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
- [x] Exibir status de sincronizacao.
- [x] Enviar vendas pendentes quando a comunicacao retornar.
- [x] Registrar conflitos de preco, produto ou estoque.
- [x] Preservar venda offline original em conflito ou falha.

Aceite minimo:

- Venda offline nao depende de comunicacao imediata.
- Venda pendente permanece preservada localmente.
- Venda sincronizada baixa estoque no servidor.
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

## Fora do MVP

- NFC-e, NF-e e integracao SEFAZ.
- TEF, PIX integrado e conciliacao automatica.
- Financeiro completo, contabilidade e RH.
- Multi-loja, multi-deposito, lote, validade e inventario ciclico formal.
- Fidelidade, cashback, campanhas automatizadas, e-commerce e aplicativo mobile nativo.
