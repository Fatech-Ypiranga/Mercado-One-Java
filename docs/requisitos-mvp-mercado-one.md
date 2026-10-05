# Levantamento de Requisitos MVP - Mercado One

## 1. Resumo Executivo

O Mercado One será um ERP para pequenos mercados varejistas, com CRM e PDV integrados. O MVP deve provar a operação essencial de uma loja física: cadastrar produtos, controlar estoque simples, registrar entradas, vender no PDV, operar parcialmente offline, manter clientes e consultar relatórios básicos.

O documento define requisitos em nível suficiente para orientar backlog, modelagem de dados e implementação futura usando a stack restrita do projeto: Java, Angular, TypeScript, HTML, CSS e SQL.

Nota de manutenção: este documento é conceitual e registra requisitos/critérios de aceite do MVP. Para saber o que já está implementado no código, consulte `docs/estado-atual.md`, `docs/backlog-mvp.md` e os READMEs dos subprojetos.

Requisitos conceituais ainda sem código correspondente: desconto simples no PDV, cancelamento pós-venda, entrada de estoque como documento multi-item, consulta administrativa de auditoria e auditoria de criação/alteração de produto. O PDV atual sempre persiste a venda localmente e sincroniza por `POST /api/offline/sales/sync`; `POST /api/sales` existe na API, mas não é o caminho da UI de caixa.

## 2. Objetivos do MVP

- Permitir que um pequeno mercado opere cadastros, estoque e vendas presenciais em um sistema único.
- Reduzir dependência de controles manuais para produtos, saldos e vendas.
- Permitir venda no PDV mesmo durante indisponibilidade temporária de internet ou servidor.
- Registrar clientes e histórico de compras para uma base inicial de CRM.
- Fornecer relatórios operacionais mínimos para acompanhamento diário.
- Preparar a base de dados para informações fiscais, sem emitir documentos fiscais no MVP.

## 3. Público-Alvo

O MVP é destinado a pequenos mercados varejistas com uma única loja por implantação inicial. O perfil esperado é uma operação com poucos usuários administrativos, um ou mais operadores de caixa e controle de estoque simples por produto e quantidade.

## 4. Atores

### 4.1 Administrador/Dono

Responsável pela configuração geral do mercado, usuários, permissões, consulta de relatórios e supervisão da operação.

### 4.2 Gerente

Responsável por cadastros, entradas de estoque, ajustes operacionais, acompanhamento de vendas e relatórios.

### 4.3 Operador de Caixa

Responsável por realizar vendas no PDV, consultar produtos, identificar clientes, registrar pagamentos manuais e finalizar vendas.

### 4.4 Atendente/Estoquista

Responsável por consultar produtos, registrar ou apoiar entradas de mercadorias e realizar ajustes de estoque quando autorizado.

## 5. Escopo Funcional

### 5.1 Gestão de Usuários e Permissões

O sistema deve permitir cadastro, edição, ativação e desativação de usuários. Cada usuário deve possuir um perfil de acesso simples, no mínimo: administrador, gerente, operador de caixa e estoquista.

Requisitos:

- Autenticar usuários antes do acesso ao sistema.
- Autorizar funcionalidades conforme perfil.
- Permitir redefinição administrativa de senha.
- Bloquear acesso de usuários inativos.
- Registrar usuário responsável em operações críticas.

### 5.2 Cadastro de Produtos

O sistema deve permitir manter produtos comercializados pelo mercado.

Requisitos:

- Cadastrar produto com nome, código de barras ou SKU, categoria, unidade, preço de venda, status e dados fiscais preparatórios.
- Editar dados cadastrais de produto.
- Ativar e inativar produto.
- Pesquisar produto por nome, código de barras ou SKU.
- Impedir uso de produto inativo em novas vendas.

Campos fiscais preparatórios sugeridos:

- NCM.
- CEST, quando aplicável.
- CFOP padrão sugerido.
- Origem da mercadoria.
- Alíquota ou classificação tributária interna, quando definida pelo mercado.

### 5.3 Cadastro de Categorias

O sistema deve permitir organizar produtos por categoria.

Requisitos:

- Cadastrar, editar, ativar e inativar categorias.
- Associar produto a uma categoria.
- Permitir consulta de produtos por categoria.

### 5.4 Cadastro de Fornecedores

O sistema deve permitir registrar fornecedores usados nas entradas de mercadorias.

Requisitos:

- Cadastrar fornecedor com nome, documento, telefone, email e observações.
- Editar e inativar fornecedor.
- Vincular fornecedor a uma entrada de estoque quando informado.

### 5.5 Cadastro de Clientes e CRM Básico

O sistema deve permitir cadastrar clientes para consulta no PDV e formação de histórico de compras.

Requisitos:

- Cadastrar cliente com nome, telefone, email, documento opcional e consentimento básico de contato.
- Pesquisar cliente por nome, telefone ou documento.
- Vincular uma venda a um cliente.
- Consultar histórico de compras do cliente.
- Permitir edição e inativação de cliente.

Ficam fora do MVP campanhas, segmentações automáticas, programas de pontos, cashback e disparos integrados de mensagens.

### 5.6 Estoque

O MVP deve controlar estoque por produto e quantidade.

Requisitos:

- Consultar saldo atual por produto.
- Registrar entrada de estoque.
- Registrar ajuste manual de estoque com justificativa.
- Registrar baixa de estoque por venda sincronizada.
- Exibir movimentações de estoque por período e produto.
- Impedir saldo negativo quando a venda estiver online, salvo se uma configuração futura permitir o contrário.

Não fazem parte do MVP controle por lote, validade, multi-depósito, endereçamento, inventário cíclico formal ou custo médio avançado.

### 5.7 Compras e Recebimento Simples

O sistema deve permitir registrar entrada de mercadorias sem implementar um módulo completo de compras.

Requisitos:

- Registrar entrada com data, fornecedor opcional, itens, quantidades e observação.
- Atualizar saldo de estoque após confirmação da entrada.
- Registrar movimentação de estoque para cada item recebido.
- Permitir consulta de entradas por período.

### 5.8 PDV Online

O PDV deve permitir a venda presencial de forma simples e rápida.

Requisitos:

- Iniciar nova venda.
- Adicionar produto por código de barras, SKU ou busca por nome.
- Alterar quantidade antes da finalização.
- Remover item antes da finalização.
- Aplicar desconto simples quando o perfil permitir.
- Cancelar venda antes da finalização.
- Identificar cliente opcionalmente.
- Registrar uma ou mais formas de pagamento manuais.
- Finalizar venda.
- Baixar estoque após finalização online confirmada.
- Gerar comprovante simples de venda sem valor fiscal.

Formas de pagamento manuais no MVP:

- Dinheiro.
- Cartão.
- PIX.
- Fiado.

O MVP não inclui TEF, integração com adquirente, geração de cobrança PIX, conciliação automática ou comprovante fiscal.

### 5.9 PDV Offline Parcial

O PDV deve permitir registrar vendas quando houver indisponibilidade temporária de comunicação com o servidor.

Requisitos:

- Manter catálogo local previamente carregado com produtos ativos e preços vigentes.
- Permitir venda offline usando os dados locais disponíveis.
- Atribuir identificador local temporário para venda offline.
- Registrar venda offline em fila local.
- Exibir status de sincronização da venda.
- Sincronizar vendas pendentes quando a comunicação retornar.
- Registrar conflitos de preço, produto ou estoque para revisão.
- Não apagar venda offline original em caso de conflito.

Estados sugeridos para venda offline:

- Pendente de sincronização.
- Sincronizada.
- Sincronizada com conflito.
- Falha de sincronização.

O MVP não precisa permitir cadastro de produtos, cadastro completo de clientes, cancelamento pós-venda ou ajuste de estoque em modo offline.

### 5.10 Relatórios Operacionais

O sistema deve fornecer relatórios mínimos para acompanhamento da operação.

Requisitos:

- Vendas por período.
- Produtos mais vendidos.
- Saldo atual de estoque.
- Movimentações de estoque por período.
- Vendas por operador.
- Vendas vinculadas a clientes.

Os relatórios devem permitir filtros básicos por período e, quando aplicável, produto, categoria, operador ou cliente.

## 6. Casos de Uso

### UC01 - Gerenciar Usuários

Ator principal: Administrador/Dono.

Fluxo principal:

1. Administrador acessa a área de usuários.
2. Sistema lista usuários existentes.
3. Administrador cria ou edita um usuário.
4. Administrador define perfil de acesso.
5. Sistema salva o usuário e aplica permissões.

Critérios de aceite:

- Usuário inativo não consegue autenticar.
- Operador de caixa não acessa funcionalidades administrativas.
- Operações críticas registram usuário responsável.

### UC02 - Cadastrar Produto

Ator principal: Gerente.

Fluxo principal:

1. Gerente acessa cadastro de produtos.
2. Informa nome, código, categoria, unidade, preço, status e dados fiscais preparatórios.
3. Sistema valida campos obrigatórios.
4. Sistema salva produto.
5. Produto ativo fica disponível para venda e consulta.

Critérios de aceite:

- Produto ativo aparece no PDV.
- Produto inativo não aparece para nova venda.
- Produto pode ser encontrado por nome, SKU ou código de barras.

### UC03 - Registrar Entrada de Estoque

Ator principal: Gerente ou Estoquista.

Fluxo principal:

1. Usuário inicia uma entrada de estoque.
2. Seleciona fornecedor opcional.
3. Adiciona produtos e quantidades.
4. Confirma a entrada.
5. Sistema atualiza saldos e registra movimentações.

Critérios de aceite:

- Saldo aumenta conforme quantidades confirmadas.
- Cada item gera movimentação de estoque.
- Entrada fica disponível para consulta por período.

### UC04 - Realizar Venda Online no PDV

Ator principal: Operador de Caixa.

Fluxo principal:

1. Operador inicia nova venda.
2. Adiciona produtos ao carrinho.
3. Sistema calcula total.
4. Operador identifica cliente, se aplicável.
5. Operador registra pagamento manual.
6. Operador finaliza venda.
7. Sistema registra venda e baixa estoque.

Critérios de aceite:

- Venda finalizada possui itens, valores, operador, pagamentos e horário.
- Estoque é reduzido para os produtos vendidos.
- Venda vinculada a um cliente aparece no histórico do cliente.

### UC05 - Realizar Venda Offline Parcial

Ator principal: Operador de Caixa.

Fluxo principal:

1. PDV identifica indisponibilidade de comunicação.
2. Operador inicia venda usando catálogo local.
3. Operador adiciona produtos disponíveis localmente.
4. Operador registra pagamento manual.
5. Sistema grava venda na fila offline com identificador local.
6. Sistema exibe venda como pendente de sincronização.

Critérios de aceite:

- Venda offline não depende de comunicação imediata com servidor.
- Venda recebe status pendente de sincronização.
- Dados mínimos da venda ficam preservados localmente.

### UC06 - Sincronizar Vendas Offline

Ator principal: Sistema.

Fluxo principal:

1. Sistema detecta retorno de comunicação.
2. Sistema envia vendas pendentes ao servidor.
3. Servidor registra a venda.
4. Servidor baixa estoque quando a venda é aceita.
5. Sistema atualiza status local.
6. Conflitos são registrados para revisão.

Critérios de aceite:

- Venda sincronizada passa a constar nos relatórios.
- Venda sincronizada baixa estoque.
- Conflito não apaga a venda original.
- Falha de sincronização mantém venda pendente ou marcada como falha.

### UC07 - Consultar Cliente e Histórico

Ator principal: Operador de Caixa ou Gerente.

Fluxo principal:

1. Usuário pesquisa cliente por nome, telefone ou documento.
2. Sistema exibe dados do cliente.
3. Usuário consulta histórico de compras.
4. No PDV, o operador vincula o cliente a uma venda.

Critérios de aceite:

- Cliente pode ser vinculado a venda.
- Histórico lista compras vinculadas ao cliente.
- Cliente inativo não deve ser sugerido para novas vendas.

### UC08 - Consultar Relatórios Operacionais

Ator principal: Administrador/Dono ou Gerente.

Fluxo principal:

1. Usuário acessa área de relatórios.
2. Seleciona tipo de relatório.
3. Aplica filtros.
4. Sistema exibe resultados.

Critérios de aceite:

- Relatório de vendas filtra por período.
- Relatório de produtos mais vendidos considera vendas finalizadas e sincronizadas.
- Relatório de estoque apresenta saldo atual.
- Operador de caixa não acessa relatórios gerenciais, salvo se autorizado.

## 7. Entidades Conceituais

### Usuário

Representa pessoa autorizada a acessar o Mercado One.

Campos conceituais:

- Identificador.
- Nome.
- Email ou login.
- Senha protegida.
- Perfil.
- Status.
- Data de criação.

### Perfil

Representa conjunto simples de permissões.

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
- Código de barras.
- SKU.
- Categoria.
- Unidade.
- Preço de venda.
- Status.
- Dados fiscais preparatórios.

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
- Observações.
- Status.

### Cliente

Pessoa identificada para relacionamento e histórico de compras.

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
- Identificador local temporário, quando offline.
- Operador.
- Cliente opcional.
- Itens.
- Pagamentos.
- Total bruto.
- Desconto.
- Total líquido.
- Status da venda.
- Status de sincronização.
- Data e hora.

### Item de Venda

Produto e quantidade vendidos dentro de uma venda.

Campos conceituais:

- Venda.
- Produto.
- Quantidade.
- Preço unitário praticado.
- Desconto do item, quando aplicável.
- Total do item.

### Pagamento

Registro manual da forma usada para pagar uma venda.

Campos conceituais:

- Venda.
- Forma de pagamento.
- Valor.
- Observação, quando aplicável.

### Movimentação de Estoque

Registro imutável de alteração de saldo de estoque.

Campos conceituais:

- Produto.
- Tipo de movimentação.
- Quantidade.
- Origem.
- Usuário responsável ou processo.
- Justificativa.
- Data e hora.

Tipos iniciais:

- Entrada.
- Saída por venda.
- Ajuste manual.

### Fila Offline

Registro local das vendas feitas sem comunicação com o servidor.

Campos conceituais:

- Identificador local.
- Dados da venda.
- Status de sincronização.
- Número de tentativas.
- Última tentativa.
- Mensagem de erro ou conflito.

## 8. Regras de Negócio

- Produto inativo não deve aparecer para nova venda.
- Cliente inativo não deve ser sugerido para nova venda.
- Venda online finalizada deve reduzir estoque imediatamente após confirmação.
- Venda offline deve reduzir estoque somente após sincronização aceita pelo servidor.
- Venda offline deve receber identificador local temporário.
- Venda offline deve manter status de sincronização visível.
- Conflitos de preço, produto ou estoque devem ser registrados para revisão.
- Conflito de sincronização não deve apagar a venda offline original.
- Histórico do cliente deve ser derivado de vendas vinculadas.
- Ajuste manual de estoque deve exigir justificativa.
- Operações críticas devem registrar usuário responsável e data/hora.
- O sistema não deve emitir documento fiscal no MVP.

## 9. Requisitos Não Funcionais

### 9.1 Tecnologia

- Backend em Java.
- Frontend em Angular com TypeScript, HTML e CSS.
- Banco de dados SQL relacional.
- Interface web responsiva para áreas administrativas.
- PDV otimizado para uso em tela de caixa.

### 9.2 Segurança

- Autenticação obrigatória.
- Senhas armazenadas de forma protegida.
- Autorização por perfil.
- Bloqueio de usuários inativos.
- Validação de entradas no frontend e backend.
- Auditoria básica de operações críticas.

### 9.3 Disponibilidade e Offline

- O PDV deve indicar quando estiver offline.
- O PDV deve manter catálogo local previamente carregado.
- Vendas offline devem ser preservadas até sincronização bem-sucedida ou revisão de falha.
- O sistema deve evitar perda silenciosa de vendas.

### 9.4 Usabilidade

- O fluxo de venda deve exigir poucos passos.
- Busca por produto deve aceitar código e nome.
- Mensagens de erro devem ser claras para usuários operacionais.
- Telas de cadastro devem destacar campos obrigatórios.

### 9.5 Auditoria

Devem ser auditadas, no mínimo:

- Criação e alteração de produto.
- Entrada de estoque.
- Ajuste manual de estoque.
- Finalização de venda.
- Sincronização de venda offline.
- Alteração de usuário ou perfil.

## 10. Fora de Escopo do MVP

- Emissão de NFC-e ou NF-e.
- Integração fiscal com SEFAZ.
- TEF.
- PIX integrado.
- Conciliação financeira.
- Contabilidade.
- Contas a pagar e receber completas.
- RH e folha de pagamento.
- Multi-loja.
- Multi-depósito.
- Controle por lote e validade.
- Programa de fidelidade.
- Cashback.
- Campanhas promocionais automatizadas.
- Disparo de mensagens por WhatsApp, SMS ou email.
- E-commerce.
- Aplicativo mobile nativo.

## 11. Critérios Gerais de Aceite

- Deve ser possível cadastrar um produto ativo e vendê-lo no PDV.
- Deve ser possível registrar entrada de estoque e visualizar saldo atualizado.
- Venda online finalizada deve aparecer em relatório de vendas.
- Venda online finalizada deve gerar baixa de estoque.
- Venda offline deve ser registrada em fila local.
- Venda offline deve sincronizar quando a comunicação voltar.
- Venda offline sincronizada deve aparecer em relatórios e baixar estoque.
- Venda vinculada a cliente deve aparecer no histórico do cliente.
- Perfis devem restringir acesso a funcionalidades sensíveis.
- Dados fiscais devem existir apenas como preparação cadastral, sem emissão fiscal.

## 12. Sugestão de Priorização para Backlog

### Prioridade 1 - Base Operacional

- Autenticação e perfis.
- Cadastro de produtos.
- Cadastro de categorias.
- Cadastro de usuários.
- Estoque simples.

### Prioridade 2 - Venda Online

- PDV online.
- Registro manual de pagamentos.
- Baixa de estoque por venda.
- Relatório de vendas por período.

### Prioridade 3 - Entradas e CRM Básico

- Cadastro de fornecedores.
- Entrada de estoque.
- Cadastro de clientes.
- Vínculo de cliente na venda.
- Histórico de compras.

### Prioridade 4 - Offline Parcial

- Catálogo local do PDV.
- Fila offline.
- Sincronização de vendas.
- Registro de conflitos.

### Prioridade 5 - Relatórios Complementares

- Produtos mais vendidos.
- Saldo de estoque.
- Movimentações de estoque.
- Vendas por operador.

## 13. Riscos e Decisões Futuras

- Offline parcial aumenta a complexidade do PDV e deve ser validado cedo com protótipo técnico.
- Fiscal sem emissão reduz escopo, mas os campos preparatórios devem ser definidos com cuidado para não bloquear NFC-e/NF-e futuramente.
- Controle de estoque sem lote e validade atende o MVP, mas pode ser insuficiente para mercados com perecíveis.
- Pagamento manual acelera o MVP, mas TEF e PIX integrado devem ser avaliados antes de operação em escala.
- Multi-loja está fora do MVP; a modelagem inicial deve evitar acoplamento desnecessário a uma única loja caso expansão seja provável.

## 14. Premissas

- O MVP atende pequenos mercados com uma loja por implantação.
- O foco inicial é operação de loja, não financeiro completo.
- O PDV offline parcial usa catálogo e preços previamente carregados.
- Vendas offline são preservadas localmente até sincronização.
- O sistema registra pagamentos manualmente, sem confirmar transações externas.
- Dados fiscais são apenas preparatórios no MVP.
- O documento será usado como base para backlog, modelagem de dados e arquitetura inicial.
