# Mercado One

Mercado One e o contexto de dominio de um ERP para pequenos mercados varejistas. Este glossario padroniza a linguagem usada em requisitos, backlog, documentacao e implementacao.

## Language

**Mercado**:
Estabelecimento varejista de pequeno porte que vende produtos ao consumidor final em uma loja fisica.
_Avoid_: Loja, cliente da plataforma

**MVP**:
Primeira versao operacional do Mercado One, limitada aos fluxos essenciais de cadastros, estoque, vendas presenciais, offline parcial, clientes e relatorios basicos.
_Avoid_: Versao completa, produto final

**Administrador**:
Pessoa responsavel pela configuracao geral, usuarios, permissoes e supervisao da operacao do mercado.
_Avoid_: Dono quando o papel no sistema for mais amplo

**Gerente**:
Pessoa responsavel por cadastros, entradas de estoque, ajustes operacionais e acompanhamento de relatorios.
_Avoid_: Supervisor

**Operador de Caixa**:
Pessoa que usa o PDV para registrar vendas presenciais.
_Avoid_: Caixa

**Estoquista**:
Pessoa que consulta produtos e apoia entradas ou ajustes de estoque quando autorizada.
_Avoid_: Repositor

**PDV**:
Ponto de venda usado pelo operador de caixa para registrar itens, informar pagamento e finalizar uma venda presencial.
_Avoid_: Caixa, checkout

**Venda**:
Operacao comercial finalizada no PDV, composta por itens vendidos, valores, pagamentos, operador responsavel e estado de sincronizacao quando ocorrer offline.
_Avoid_: Pedido, transacao

**Item de Venda**:
Produto e quantidade vendidos dentro de uma venda, com preco praticado e total proprio.
_Avoid_: Linha, item

**Pagamento**:
Registro manual da forma e do valor usados para quitar uma venda no MVP.
_Avoid_: Cobranca, transacao financeira

**Cliente**:
Pessoa identificada no Mercado One para fins de historico de compras e relacionamento comercial.
_Avoid_: Consumidor, comprador, contato

**Produto**:
Item comercializavel pelo mercado, com identificacao, categoria, unidade, preco de venda, dados fiscais preparatorios e status.
_Avoid_: Mercadoria, item cadastral

**Categoria**:
Agrupamento operacional usado para organizar produtos.
_Avoid_: Grupo, familia

**Fornecedor**:
Pessoa juridica ou fisica de quem o mercado compra produtos para revenda.
_Avoid_: Parceiro, distribuidor

**Estoque**:
Saldo operacional de produtos disponiveis para venda, controlado por produto e quantidade no MVP.
_Avoid_: Inventario, almoxarifado

**Entrada de Estoque**:
Registro de chegada de produtos ao mercado, com fornecedor opcional, itens, quantidades e observacao.
_Avoid_: Compra completa, recebimento fiscal

**Ajuste de Estoque**:
Alteracao manual de saldo feita por usuario autorizado, sempre com justificativa.
_Avoid_: Correcao solta, acerto

**Movimentacao de Estoque**:
Registro imutavel de alteracao no saldo de um produto, originada por entrada, venda sincronizada ou ajuste manual.
_Avoid_: Lancamento, baixa

**Fila Offline**:
Conjunto de vendas registradas localmente pelo PDV sem comunicacao imediata com o servidor, aguardando sincronizacao.
_Avoid_: Cache, fila local

**Sincronizacao**:
Processo que envia vendas offline ao servidor, confirma sua persistencia e registra conflitos sem apagar a venda original.
_Avoid_: Atualizacao, upload

**Conflito de Sincronizacao**:
Situacao em que uma venda offline nao pode ser aceita automaticamente por diferenca de preco, produto, pagamento ou estoque.
_Avoid_: Erro generico, falha tecnica

**Auditoria**:
Registro imutavel de operacoes criticas do mercado no servidor.
_Avoid_: Log tecnico, historico generico

**Dados Fiscais Preparatorios**:
Campos cadastrais que deixam o produto preparado para futura emissao fiscal, sem emitir NFC-e ou NF-e no MVP.
_Avoid_: Emissao fiscal, modulo fiscal
