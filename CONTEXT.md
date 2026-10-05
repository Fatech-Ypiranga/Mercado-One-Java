# Mercado One

Mercado One é o contexto de domínio de um ERP para pequenos mercados varejistas. Este glossário padroniza a linguagem usada em requisitos, backlog, documentação e implementação.

## Language

**Mercado**:
Estabelecimento varejista de pequeno porte que vende produtos ao consumidor final em uma loja física.
_Avoid_: Loja, cliente da plataforma

**MVP**:
Primeira versão operacional do Mercado One, limitada aos fluxos essenciais de cadastros, estoque, vendas presenciais, offline parcial, clientes e relatórios básicos.
_Avoid_: Versão completa, produto final

**Administrador**:
Pessoa responsável pela configuração geral, usuários, permissões e supervisão da operação do mercado.
_Avoid_: Dono quando o papel no sistema for mais amplo

**Gerente**:
Pessoa responsável por cadastros, entradas de estoque, ajustes operacionais e acompanhamento de relatórios.
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
Operação comercial finalizada no PDV, composta por itens vendidos, valores, pagamentos, operador responsável e estado de sincronização quando a venda ocorrer offline.
_Avoid_: Pedido, transação

**Item de Venda**:
Produto e quantidade vendidos dentro de uma venda, com preço praticado e total próprio.
_Avoid_: Linha, item

**Pagamento**:
Registro manual da forma e do valor usados para quitar uma venda no MVP.
_Avoid_: Cobrança, transação financeira

**Cliente**:
Pessoa identificada no Mercado One para fins de histórico de compras e relacionamento comercial.
_Avoid_: Consumidor, comprador, contato

**Produto**:
Item comercializável pelo mercado, com identificação, categoria, unidade, preço de venda, dados fiscais preparatórios e status.
_Avoid_: Mercadoria, item cadastral

**Categoria**:
Agrupamento operacional usado para organizar produtos.
_Avoid_: Grupo, família

**Fornecedor**:
Pessoa jurídica ou física de quem o mercado compra produtos para revenda.
_Avoid_: Parceiro, distribuidor

**Estoque**:
Saldo operacional de produtos disponíveis para venda, controlado por produto e quantidade no MVP.
_Avoid_: Inventário, almoxarifado

**Entrada de Estoque**:
Registro de chegada de produtos ao mercado, com fornecedor opcional, itens, quantidades e observação.
_Avoid_: Compra completa, recebimento fiscal

**Ajuste de Estoque**:
Alteração manual de saldo feita por usuário autorizado, sempre com justificativa.
_Avoid_: Correção solta, acerto

**Movimentação de Estoque**:
Registro imutável de alteração no saldo de um produto, originada por entrada, venda sincronizada ou ajuste manual.
_Avoid_: Lançamento, baixa

**Fila Offline**:
Conjunto de vendas registradas localmente pelo PDV sem comunicação imediata com o servidor, aguardando sincronização.
_Avoid_: Cache, fila local

**Sincronização**:
Processo que envia vendas offline ao servidor, confirma sua persistência e registra conflitos sem apagar a venda original.
_Avoid_: Atualização, upload

**Conflito de Sincronização**:
Situação em que uma venda offline não pode ser aceita automaticamente por diferença de preço, produto, pagamento ou estoque.
_Avoid_: Erro genérico, falha técnica

**Auditoria**:
Registro imutável de operações críticas do mercado no servidor.
_Avoid_: Log técnico, histórico genérico

**Dados Fiscais Preparatórios**:
Campos cadastrais que deixam o produto preparado para futura emissão fiscal, sem emitir NFC-e ou NF-e no MVP.
_Avoid_: Emissão fiscal, módulo fiscal
