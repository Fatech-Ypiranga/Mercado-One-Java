# Uso do admin web

Este guia descreve o que cada pessoa da loja faz no administrativo. A instalacao do ambiente fica em [Desenvolvimento local](desenvolvimento-local.md). O que o sistema ainda nao faz fica em [Estado atual](estado-atual.md).

## Quem entra

O admin atende tres perfis. O menu lateral so mostra o que o perfil pode abrir.

| Perfil | O que ve |
| --- | --- |
| Administrador | Inicio, vendas, conflitos offline, produtos, categorias, estoque, fornecedores, clientes e usuarios |
| Gerente | O mesmo, exceto usuarios |
| Estoquista | Inicio e estoque |

O operador de caixa nao tem tela aqui. A venda presencial fica no [PDV](uso-pdv.md).

## Entrar e sair

1. Abra a tela **Entrar no admin**.
2. Informe o login e a senha que o administrador da loja passou.
3. Se o login ou a senha estiverem errados, a tela avisa `Login ou senha inválidos.`
4. Se a API nao responder, a tela avisa que nao foi possivel conectar.
5. Depois do acesso, o painel inferior mostra o nome, o perfil e o botao **Sair**.

No ambiente de desenvolvimento o campo de login ja vem preenchido com `admin`. A senha desse ambiente esta em [Desenvolvimento local](desenvolvimento-local.md), nao neste guia.

## Inicio

A tela **Inicio** mostra a data, o usuario e a versao da API. Se a API nao responder, o carimbo fica `API indisponível`.

Administrador e gerente veem conflitos pendentes do PDV e os totais de venda do dia. Cada conflito pendente abre a tela **Offline**.

O estoquista ve o inicio, mas nao ve esses totais nem a lista de conflitos. A propria tela diz para usar o estoque.

## Usuarios

Somente o administrador. Tela **Usuários e perfis**.

1. Filtre por nome, login, perfil ou status e use **Filtrar**.
2. Cadastre em **Novo usuário** ou escolha **Editar**.
3. Informe nome, login, perfil (`ADMIN`, `GERENTE`, `OPERADOR_CAIXA` ou `ESTOQUISTA`) e se o usuario fica ativo.
4. Na criacao, defina uma senha com pelo menos 6 caracteres. Na edicao, a senha e opcional: deixe em branco para manter a atual.
5. Salve. A lista passa a mostrar o usuario.

Um usuario inativo nao consegue entrar. O operador de caixa criado aqui usa o PDV, nao este admin.

## Categorias e produtos

Administrador e gerente.

**Categorias** organiza os agrupamentos dos produtos. Busque pelo nome, filtre ativos ou inativos e grave a ficha ao lado da lista.

**Produtos** guarda o item vendavel: nome, categoria, unidade, preco de venda, SKU, codigo de barras, status e dados fiscais so de preparo. Nao ha emissao de nota nesta tela.

1. Filtre por nome, SKU, codigo, categoria ou status.
2. Cadastre ou edite na ficha ao lado.
3. Deixe inativo o produto que nao deve mais entrar em venda nova.

Produto inativo continua consultavel no filtro de inativos, mas o PDV nao o oferece para uma venda nova.

## Estoque

Administrador, gerente e estoquista. Tela **Saldos e movimentações**.

A lista mostra o saldo atual. O historico abaixo mostra as movimentacoes do filtro.

Para registrar uma movimentacao:

1. Escolha **Entrada** ou **Ajuste manual**.
2. Selecione um produto ativo.
3. Na entrada, informe a quantidade, o fornecedor se houver, e o documento se houver. A entrada e de um produto por vez.
4. No ajuste, informe o novo saldo e a justificativa. Sem justificativa o formulario nao segue.
5. Use **Registrar**.

O saldo sobe na entrada confirmada. O ajuste substitui o saldo pelo valor informado e exige o motivo. Cada alteracao entra no historico. Nao ha, nesta tela, um documento unico com varios produtos.

## Vendas

Administrador e gerente. Tela **Relatório de vendas**.

Filtre por periodo, identificador do operador, status e identificador do cliente. O unico status disponivel e **Confirmada**. Nao ha cancelamento de venda depois de confirmada.

**Filtrar** atualiza a lista e os totais. **CSV** baixa o relatorio do filtro atual. **Anterior** e **Próxima** trocam a pagina.

Mais abaixo, **Produtos mais vendidos** usa o mesmo periodo.

O historico de um cliente tambem abre daqui. Na tela de clientes, o botao **Vendas** chega neste relatorio ja filtrado por aquele cliente.

## Conflitos do PDV

Administrador e gerente. Tela **Conflitos de sincronização**.

Uma venda do caixa pode ficar pendente quando o servidor nao aceita o preco, o produto, o pagamento ou o estoque. A tela lista essas pendencias.

1. Abra o conflito. A venda local aparece com o motivo.
2. Para aceitar, use **Aceitar**. A observacao e opcional. O servidor registra a venda com o preco praticado no PDV e baixa o estoque.
3. Para rejeitar, preencha **Observação da decisão** e use **Rejeitar**. Sem essa observacao a rejeicao nao segue.
4. Use **Atualizar** para reler a lista.

Aceitar ou rejeitar encerra a pendencia neste admin. O caixa nao muda sozinho: no PDV a venda continua marcada como conflito. Avise o operador se a loja precisar tratar o comprovante local.

## Clientes

Administrador e gerente. Tela **Clientes**.

Busque por nome, telefone, e-mail ou documento. Cadastre ou edite nome, telefone, e-mail, documento e se o cliente esta ativo. Cliente inativo nao deve ser escolhido numa venda nova do PDV.

O botao **Vendas** abre o relatorio filtrado por aquele cliente. Nao ha uma ficha separada de historico.

## Fornecedores

Administrador e gerente. Tela **Fornecedores**.

Cadastre nome, documento, telefone, e-mail e status. O fornecedor ativo pode ser escolhido na entrada de estoque. O vinculo e opcional: a entrada tambem aceita ficar sem fornecedor.
