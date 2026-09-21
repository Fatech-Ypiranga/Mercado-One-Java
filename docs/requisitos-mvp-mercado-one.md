# Levantamento de Requisitos MVP - Mercado One

## 1. Resumo Executivo

O Mercado One sera um ERP para pequenos mercados varejistas, com CRM e PDV integrados. O MVP deve provar a operacao essencial de uma loja fisica: cadastrar produtos, controlar estoque simples, registrar entradas, vender no PDV, operar parcialmente offline, manter clientes e consultar relatorios basicos.

O documento define requisitos em nivel suficiente para orientar backlog, modelagem de dados e implementacao futura usando a stack restrita do projeto: Java, Angular, TypeScript, HTML, CSS e SQL.

Nota de manutencao: este documento e conceitual e registra requisitos/criterios de aceite do MVP. Para saber o que ja esta implementado no codigo, consulte `docs/estado-atual.md`, `docs/backlog-mvp.md` e os READMEs dos subprojetos.

Requisitos conceituais ainda sem codigo correspondente: desconto simples no PDV, cancelamento pos-venda, entrada de estoque como documento multi-item, consulta administrativa de auditoria e auditoria de criacao/alteracao de produto. O PDV atual sempre persiste a venda localmente e sincroniza por `POST /api/offline/sales/sync`; `POST /api/sales` existe na API, mas nao e o caminho da UI de caixa.

## 2. Objetivos do MVP

- Permitir que um pequeno mercado opere cadastros, estoque e vendas presenciais em um sistema unico.
- Reduzir dependencia de controles manuais para produtos, saldos e vendas.
- Permitir venda no PDV mesmo durante indisponibilidade temporaria de internet ou servidor.
- Registrar clientes e historico de compras para uma base inicial de CRM.
- Fornecer relatorios operacionais minimos para acompanhamento diario.
- Preparar a base de dados para informacoes fiscais, sem emitir documentos fiscais no MVP.

## 3. Publico-Alvo

O MVP e destinado a pequenos mercados varejistas com uma unica loja por implantacao inicial. O perfil esperado e uma operacao com poucos usuarios administrativos, um ou mais operadores de caixa e controle de estoque simples por produto e quantidade.

## 4. Atores

### 4.1 Administrador/Dono

Responsavel pela configuracao geral do mercado, usuarios, permissoes, consulta de relatorios e supervisao da operacao.

### 4.2 Gerente

Responsavel por cadastros, entradas de estoque, ajustes operacionais, acompanhamento de vendas e relatorios.

### 4.3 Operador de Caixa

Responsavel por realizar vendas no PDV, consultar produtos, identificar clientes, registrar pagamentos manuais e finalizar vendas.

### 4.4 Atendente/Estoquista

Responsavel por consultar produtos, registrar ou apoiar entradas de mercadorias e realizar ajustes de estoque quando autorizado.

## 5. Escopo Funcional

### 5.1 Gestao de Usuarios e Permissoes

O sistema deve permitir cadastro, edicao, ativacao e desativacao de usuarios. Cada usuario deve possuir um perfil de acesso simples, no minimo: administrador, gerente, operador de caixa e estoquista.

Requisitos:

- Autenticar usuarios antes do acesso ao sistema.
- Autorizar funcionalidades conforme perfil.
- Permitir redefinicao administrativa de senha.
- Bloquear acesso de usuarios inativos.
- Registrar usuario responsavel em operacoes criticas.

### 5.2 Cadastro de Produtos

O sistema deve permitir manter produtos comercializados pelo mercado.

Requisitos:

- Cadastrar produto com nome, codigo de barras ou SKU, categoria, unidade, preco de venda, status e dados fiscais preparatorios.
- Editar dados cadastrais de produto.
- Ativar e inativar produto.
- Pesquisar produto por nome, codigo de barras ou SKU.
- Impedir uso de produto inativo em novas vendas.

Campos fiscais preparatorios sugeridos:

- NCM.
- CEST, quando aplicavel.
- CFOP padrao sugerido.
- Origem da mercadoria.
- Aliquota ou classificacao tributaria interna, quando definida pelo mercado.

### 5.3 Cadastro de Categorias

O sistema deve permitir organizar produtos por categoria.

Requisitos:

- Cadastrar, editar, ativar e inativar categorias.
- Associar produto a uma categoria.
- Permitir consulta de produtos por categoria.

### 5.4 Cadastro de Fornecedores

O sistema deve permitir registrar fornecedores usados nas entradas de mercadorias.

Requisitos:

- Cadastrar fornecedor com nome, documento, telefone, email e observacoes.
- Editar e inativar fornecedor.
- Vincular fornecedor a uma entrada de estoque quando informado.

### 5.5 Cadastro de Clientes e CRM Basico

O sistema deve permitir cadastrar clientes para consulta no PDV e formacao de historico de compras.

Requisitos:

- Cadastrar cliente com nome, telefone, email, documento opcional e consentimento basico de contato.
- Pesquisar cliente por nome, telefone ou documento.
- Vincular uma venda a um cliente.
- Consultar historico de compras do cliente.
- Permitir edicao e inativacao de cliente.

Ficam fora do MVP campanhas, segmentacoes automaticas, programas de pontos, cashback e disparos integrados de mensagens.

### 5.6 Estoque

O MVP deve controlar estoque por produto e quantidade.

Requisitos:

- Consultar saldo atual por produto.
- Registrar entrada de estoque.
- Registrar ajuste manual de estoque com justificativa.
- Registrar baixa de estoque por venda sincronizada.
- Exibir movimentacoes de estoque por periodo e produto.
- Impedir saldo negativo quando a venda estiver online, salvo se uma configuracao futura permitir o contrario.

Nao fazem parte do MVP controle por lote, validade, multi-deposito, enderecamento, inventario ciclico formal ou custo medio avancado.

### 5.7 Compras e Recebimento Simples

O sistema deve permitir registrar entrada de mercadorias sem implementar um modulo completo de compras.

Requisitos:

- Registrar entrada com data, fornecedor opcional, itens, quantidades e observacao.
- Atualizar saldo de estoque apos confirmacao da entrada.
- Registrar movimentacao de estoque para cada item recebido.
- Permitir consulta de entradas por periodo.

### 5.8 PDV Online

O PDV deve permitir a venda presencial de forma simples e rapida.

Requisitos:

- Iniciar nova venda.
- Adicionar produto por codigo de barras, SKU ou busca por nome.
- Alterar quantidade antes da finalizacao.
- Remover item antes da finalizacao.
- Aplicar desconto simples quando o perfil permitir.
- Cancelar venda antes da finalizacao.
- Identificar cliente opcionalmente.
- Registrar uma ou mais formas de pagamento manuais.
- Finalizar venda.
- Baixar estoque apos finalizacao online confirmada.
- Gerar comprovante simples de venda sem valor fiscal.

Formas de pagamento manuais no MVP:

- Dinheiro.
- Cartao.
- PIX.
- Fiado.

O MVP nao inclui TEF, integracao com adquirente, geracao de cobranca PIX, conciliacao automatica ou comprovante fiscal.

### 5.9 PDV Offline Parcial

O PDV deve permitir registrar vendas quando houver indisponibilidade temporaria de comunicacao com o servidor.

Requisitos:

- Manter catalogo local previamente carregado com produtos ativos e precos vigentes.
- Permitir venda offline usando os dados locais disponiveis.
- Atribuir identificador local temporario para venda offline.
- Registrar venda offline em fila local.
- Exibir status de sincronizacao da venda.
- Sincronizar vendas pendentes quando a comunicacao retornar.
- Registrar conflitos de preco, produto ou estoque para revisao.
- Nao apagar venda offline original em caso de conflito.

Estados sugeridos para venda offline:

- Pendente de sincronizacao.
- Sincronizada.
- Sincronizada com conflito.
- Falha de sincronizacao.

O MVP nao precisa permitir cadastro de produtos, cadastro completo de clientes, cancelamento pos-venda ou ajuste de estoque em modo offline.

### 5.10 Relatorios Operacionais

O sistema deve fornecer relatorios minimos para acompanhamento da operacao.

Requisitos:

- Vendas por periodo.
- Produtos mais vendidos.
- Saldo atual de estoque.
- Movimentacoes de estoque por periodo.
- Vendas por operador.
- Vendas vinculadas a clientes.

Os relatorios devem permitir filtros basicos por periodo e, quando aplicavel, produto, categoria, operador ou cliente.

## 6. Casos de Uso

### UC01 - Gerenciar Usuarios

Ator principal: Administrador/Dono.

Fluxo principal:

1. Administrador acessa a area de usuarios.
2. Sistema lista usuarios existentes.
3. Administrador cria ou edita um usuario.
4. Administrador define perfil de acesso.
5. Sistema salva o usuario e aplica permissoes.

Criterios de aceite:

- Usuario inativo nao consegue autenticar.
- Operador de caixa nao acessa funcionalidades administrativas.
- Operacoes criticas registram usuario responsavel.

### UC02 - Cadastrar Produto

Ator principal: Gerente.

Fluxo principal:

1. Gerente acessa cadastro de produtos.
2. Informa nome, codigo, categoria, unidade, preco, status e dados fiscais preparatorios.
3. Sistema valida campos obrigatorios.
4. Sistema salva produto.
5. Produto ativo fica disponivel para venda e consulta.

Criterios de aceite:

- Produto ativo aparece no PDV.
- Produto inativo nao aparece para nova venda.
- Produto pode ser encontrado por nome, SKU ou codigo de barras.

### UC03 - Registrar Entrada de Estoque

Ator principal: Gerente ou Estoquista.

Fluxo principal:

1. Usuario inicia uma entrada de estoque.
2. Seleciona fornecedor opcional.
3. Adiciona produtos e quantidades.
4. Confirma a entrada.
5. Sistema atualiza saldos e registra movimentacoes.

Criterios de aceite:

- Saldo aumenta conforme quantidades confirmadas.
- Cada item gera movimentacao de estoque.
- Entrada fica disponivel para consulta por periodo.

### UC04 - Realizar Venda Online no PDV

Ator principal: Operador de Caixa.

Fluxo principal:

1. Operador inicia nova venda.
2. Adiciona produtos ao carrinho.
3. Sistema calcula total.
4. Operador identifica cliente, se aplicavel.
5. Operador registra pagamento manual.
6. Operador finaliza venda.
7. Sistema registra venda e baixa estoque.

Criterios de aceite:

- Venda finalizada possui itens, valores, operador, pagamentos e horario.
- Estoque e reduzido para os produtos vendidos.
- Venda vinculada a cliente aparece no historico do cliente.

### UC05 - Realizar Venda Offline Parcial

Ator principal: Operador de Caixa.

Fluxo principal:

1. PDV identifica indisponibilidade de comunicacao.
2. Operador inicia venda usando catalogo local.
3. Operador adiciona produtos disponiveis localmente.
4. Operador registra pagamento manual.
5. Sistema grava venda na fila offline com identificador local.
6. Sistema exibe venda como pendente de sincronizacao.

Criterios de aceite:

- Venda offline nao depende de comunicacao imediata com servidor.
- Venda recebe status pendente de sincronizacao.
- Dados minimos da venda ficam preservados localmente.

### UC06 - Sincronizar Vendas Offline

Ator principal: Sistema.

Fluxo principal:

1. Sistema detecta retorno de comunicacao.
2. Sistema envia vendas pendentes ao servidor.
3. Servidor registra a venda.
4. Servidor baixa estoque quando a venda e aceita.
5. Sistema atualiza status local.
6. Conflitos sao registrados para revisao.

Criterios de aceite:

- Venda sincronizada passa a constar nos relatorios.
- Venda sincronizada baixa estoque.
- Conflito nao apaga a venda original.
- Falha de sincronizacao mantem venda pendente ou marcada como falha.

### UC07 - Consultar Cliente e Historico

Ator principal: Operador de Caixa ou Gerente.

Fluxo principal:

1. Usuario pesquisa cliente por nome, telefone ou documento.
2. Sistema exibe dados do cliente.
3. Usuario consulta historico de compras.
4. No PDV, operador vincula cliente a venda.

Criterios de aceite:

- Cliente pode ser vinculado a venda.
- Historico lista compras vinculadas ao cliente.
- Cliente inativo nao deve ser sugerido para novas vendas.

### UC08 - Consultar Relatorios Operacionais

Ator principal: Administrador/Dono ou Gerente.

Fluxo principal:

1. Usuario acessa area de relatorios.
2. Seleciona tipo de relatorio.
3. Aplica filtros.
4. Sistema exibe resultados.

Criterios de aceite:

- Relatorio de vendas filtra por periodo.
- Relatorio de produtos mais vendidos considera vendas finalizadas e sincronizadas.
- Relatorio de estoque apresenta saldo atual.
- Operador de caixa nao acessa relatorios gerenciais, salvo se autorizado.

## 7. Entidades Conceituais

### Usuario

Representa pessoa autorizada a acessar o Mercado One.

Campos conceituais:

- Identificador.
- Nome.
- Email ou login.
- Senha protegida.
- Perfil.
- Status.
- Data de criacao.

### Perfil

Representa conjunto simples de permissoes.

Perfis iniciais:

- Administrador.
- Gerente.
- Operador de caixa.
- Estoquista.

### Produto

Representa item vendido pelo mercado.

Campos conceituais:

- Identificador.
- Nome.
- Codigo de barras.
- SKU.
- Categoria.
- Unidade.
- Preco de venda.
- Status.
- Dados fiscais preparatorios.

### Categoria

Agrupamento operacional de produtos.

Campos conceituais:

- Identificador.
- Nome.
- Status.

### Fornecedor

Origem comercial de produtos comprados pelo mercado.

Campos conceituais:

- Identificador.
- Nome.
- Documento.
- Telefone.
- Email.
- Observacoes.
- Status.

### Cliente

Pessoa identificada para relacionamento e historico de compras.

Campos conceituais:

- Identificador.
- Nome.
- Telefone.
- Email.
- Documento.
- Consentimento de contato.
- Status.

### Venda

Registro de venda presencial realizada no PDV.

Campos conceituais:

- Identificador.
- Identificador local temporario, quando offline.
- Operador.
- Cliente opcional.
- Itens.
- Pagamentos.
- Total bruto.
- Desconto.
- Total liquido.
- Status da venda.
- Status de sincronizacao.
- Data e hora.

### Item de Venda

Produto e quantidade vendidos dentro de uma venda.

Campos conceituais:

- Venda.
- Produto.
- Quantidade.
- Preco unitario praticado.
- Desconto do item, quando aplicavel.
- Total do item.

### Pagamento

Registro manual da forma usada para pagar uma venda.

Campos conceituais:

- Venda.
- Forma de pagamento.
- Valor.
- Observacao, quando aplicavel.

### Movimentacao de Estoque

Registro imutavel de alteracao de saldo de estoque.

Campos conceituais:

- Produto.
- Tipo de movimentacao.
- Quantidade.
- Origem.
- Usuario responsavel ou processo.
- Justificativa.
- Data e hora.

Tipos iniciais:

- Entrada.
- Saida por venda.
- Ajuste manual.

### Fila Offline

Registro local das vendas feitas sem comunicacao com o servidor.

Campos conceituais:

- Identificador local.
- Dados da venda.
- Status de sincronizacao.
- Numero de tentativas.
- Ultima tentativa.
- Mensagem de erro ou conflito.

## 8. Regras de Negocio

- Produto inativo nao deve aparecer para nova venda.
- Cliente inativo nao deve ser sugerido para nova venda.
- Venda online finalizada deve reduzir estoque imediatamente apos confirmacao.
- Venda offline deve reduzir estoque somente apos sincronizacao aceita pelo servidor.
- Venda offline deve receber identificador local temporario.
- Venda offline deve manter status de sincronizacao visivel.
- Conflitos de preco, produto ou estoque devem ser registrados para revisao.
- Conflito de sincronizacao nao deve apagar a venda offline original.
- Historico do cliente deve ser derivado de vendas vinculadas.
- Ajuste manual de estoque deve exigir justificativa.
- Operacoes criticas devem registrar usuario responsavel e data/hora.
- O sistema nao deve emitir documento fiscal no MVP.

## 9. Requisitos Nao Funcionais

### 9.1 Tecnologia

- Backend em Java.
- Frontend em Angular com TypeScript, HTML e CSS.
- Banco de dados SQL relacional.
- Interface web responsiva para areas administrativas.
- PDV otimizado para uso em tela de caixa.

### 9.2 Seguranca

- Autenticacao obrigatoria.
- Senhas armazenadas de forma protegida.
- Autorizacao por perfil.
- Bloqueio de usuarios inativos.
- Validacao de entradas no frontend e backend.
- Auditoria basica de operacoes criticas.

### 9.3 Disponibilidade e Offline

- O PDV deve indicar quando estiver offline.
- O PDV deve manter catalogo local previamente carregado.
- Vendas offline devem ser preservadas ate sincronizacao bem-sucedida ou revisao de falha.
- O sistema deve evitar perda silenciosa de vendas.

### 9.4 Usabilidade

- O fluxo de venda deve exigir poucos passos.
- Busca por produto deve aceitar codigo e nome.
- Mensagens de erro devem ser claras para usuarios operacionais.
- Telas de cadastro devem destacar campos obrigatorios.

### 9.5 Auditoria

Devem ser auditadas, no minimo:

- Criacao e alteracao de produto.
- Entrada de estoque.
- Ajuste manual de estoque.
- Finalizacao de venda.
- Sincronizacao de venda offline.
- Alteracao de usuario ou perfil.

## 10. Fora de Escopo do MVP

- Emissao de NFC-e ou NF-e.
- Integracao fiscal com SEFAZ.
- TEF.
- PIX integrado.
- Conciliacao financeira.
- Contabilidade.
- Contas a pagar e receber completas.
- RH e folha de pagamento.
- Multi-loja.
- Multi-deposito.
- Controle por lote e validade.
- Programa de fidelidade.
- Cashback.
- Campanhas promocionais automatizadas.
- Disparo de mensagens por WhatsApp, SMS ou email.
- E-commerce.
- Aplicativo mobile nativo.

## 11. Criterios Gerais de Aceite

- Deve ser possivel cadastrar um produto ativo e vende-lo no PDV.
- Deve ser possivel registrar entrada de estoque e visualizar saldo atualizado.
- Venda online finalizada deve aparecer em relatorio de vendas.
- Venda online finalizada deve gerar baixa de estoque.
- Venda offline deve ser registrada em fila local.
- Venda offline deve sincronizar quando a comunicacao voltar.
- Venda offline sincronizada deve aparecer em relatorios e baixar estoque.
- Venda vinculada a cliente deve aparecer no historico do cliente.
- Perfis devem restringir acesso a funcionalidades sensiveis.
- Dados fiscais devem existir apenas como preparacao cadastral, sem emissao fiscal.

## 12. Sugestao de Priorizacao para Backlog

### Prioridade 1 - Base Operacional

- Autenticacao e perfis.
- Cadastro de produtos.
- Cadastro de categorias.
- Cadastro de usuarios.
- Estoque simples.

### Prioridade 2 - Venda Online

- PDV online.
- Registro manual de pagamentos.
- Baixa de estoque por venda.
- Relatorio de vendas por periodo.

### Prioridade 3 - Entradas e CRM Basico

- Cadastro de fornecedores.
- Entrada de estoque.
- Cadastro de clientes.
- Vinculo de cliente na venda.
- Historico de compras.

### Prioridade 4 - Offline Parcial

- Catalogo local do PDV.
- Fila offline.
- Sincronizacao de vendas.
- Registro de conflitos.

### Prioridade 5 - Relatorios Complementares

- Produtos mais vendidos.
- Saldo de estoque.
- Movimentacoes de estoque.
- Vendas por operador.

## 13. Riscos e Decisoes Futuras

- Offline parcial aumenta a complexidade do PDV e deve ser validado cedo com prototipo tecnico.
- Fiscal sem emissao reduz escopo, mas os campos preparatorios devem ser definidos com cuidado para nao bloquear NFC-e/NF-e futuramente.
- Controle de estoque sem lote e validade atende o MVP, mas pode ser insuficiente para mercados com pereciveis.
- Pagamento manual acelera o MVP, mas TEF e PIX integrado devem ser avaliados antes de operacao em escala.
- Multi-loja esta fora do MVP; a modelagem inicial deve evitar acoplamento desnecessario a uma unica loja caso expansao seja provavel.

## 14. Assumptions

- O MVP atende pequenos mercados com uma loja por implantacao.
- O foco inicial e operacao de loja, nao financeiro completo.
- O PDV offline parcial usa catalogo e precos previamente carregados.
- Vendas offline sao preservadas localmente ate sincronizacao.
- O sistema registra pagamentos manualmente, sem confirmar transacoes externas.
- Dados fiscais sao apenas preparatorios no MVP.
- O documento sera usado como base para backlog, modelagem de dados e arquitetura inicial.
