# Uso do admin web

Este guia descreve o que cada pessoa da loja faz no administrativo. A instalação do ambiente fica em [Desenvolvimento local](desenvolvimento-local.md). O que o sistema ainda não faz fica em [Estado atual](estado-atual.md).

## Quem entra

O admin atende três perfis. O menu lateral só mostra o que o perfil pode abrir.

| Perfil | O que vê |
| --- | --- |
| Administrador | Início, vendas, conflitos offline, produtos, categorias, estoque, fornecedores, clientes e usuários |
| Gerente | O mesmo, exceto usuários |
| Estoquista | Início e estoque |

O operador de caixa não tem tela aqui. A venda presencial fica no [PDV](uso-pdv.md).

No tablet e no celular, use **Menu** para abrir as áreas do administrativo. O menu fecha ao escolher uma área ou pressionar Escape; as mesmas permissões se aplicam em qualquer tamanho de tela. As listas mostram cada registro com uma ação **Editar** visível. No celular, **Novo** ou **Nova** abre o formulário; **Voltar à lista** retorna aos resultados sem alterar os filtros. Se uma gravação falhar, os dados digitados permanecem no formulário para correção e nova tentativa.

## Entrar e sair

1. Abra a tela **Entrar no admin**.
2. Informe o login e a senha que o administrador da loja passou.
3. Se o login ou a senha estiverem errados, a tela avisa `Login ou senha inválidos.`
4. Se a API não responder, a tela avisa que não foi possível conectar.
5. Depois do acesso, o painel inferior mostra o nome, o perfil e o botão **Sair**.

No ambiente de desenvolvimento, o campo de login já vem preenchido com `admin`. A senha desse ambiente está em [Desenvolvimento local](desenvolvimento-local.md), não neste guia.

## Início

A tela **Inicio** mostra a data, o usuário e a versão da API. Se a API não responder, o carimbo fica `API indisponível`.

Administrador e gerente veem conflitos pendentes do PDV e os totais de venda do dia. Cada conflito pendente abre a tela **Offline**.

O estoquista vê o início, mas não vê esses totais nem a lista de conflitos. A própria tela diz para usar o estoque.

## Usuários

Somente o administrador. Tela **Usuários e perfis**.

1. Filtre por nome, login, perfil ou status e use **Filtrar**.
2. Cadastre em **Novo usuário** ou escolha **Editar**.
3. Informe nome, login, perfil (`ADMIN`, `GERENTE`, `OPERADOR_CAIXA` ou `ESTOQUISTA`) e se o usuário fica ativo.
4. Na criação, defina uma senha com pelo menos 6 caracteres. Na edição, a senha é opcional: deixe em branco para manter a atual.
5. Salve. A lista passa a mostrar o usuário.

Um usuário inativo não consegue entrar. O operador de caixa criado aqui usa o PDV, não este admin.

## Categorias e produtos

Administrador e gerente.

A tela **Categorias** organiza os agrupamentos dos produtos. Busque pelo nome, filtre ativos ou inativos e grave a ficha ao lado da lista.

A tela **Produtos** reúne os dados do item vendável: nome, categoria, unidade, preço de venda, SKU, código de barras, status e dados fiscais preparatórios. Não há emissão de nota nesta tela.

1. Filtre por nome, SKU, código, categoria ou status.
2. Cadastre ou edite na ficha ao lado.
3. Deixe inativo o produto que não deve mais entrar em venda nova.

Produto inativo continua consultável no filtro de inativos, mas o PDV não o oferece para uma venda nova.

## Estoque

Administrador, gerente e estoquista. Tela **Saldos e movimentações**.

A lista mostra o saldo atual. O histórico abaixo mostra as movimentações do filtro.

Para registrar uma movimentação:

1. Escolha **Entrada** ou **Ajuste manual**.
2. Selecione um produto ativo.
3. Na entrada, informe a quantidade e, se houver, o fornecedor e o documento. A entrada é de um produto por vez.
4. No ajuste, informe o novo saldo e a justificativa. Sem justificativa o formulário não segue.
5. Use **Registrar**.

Os saldos, o formulário **Registrar movimentação** e o **Histórico de movimentações** ficam em seções separadas. A lista e o histórico continuam visíveis durante uma atualização, com indicação de que os dados estão sendo atualizados.

O saldo sobe na entrada confirmada. O ajuste substitui o saldo pelo valor informado e exige o motivo. Cada alteração entra no histórico. Não há, nesta tela, um documento único com vários produtos.

## Vendas

Administrador e gerente. Tela **Relatório de vendas**.

Filtre por período, identificador do operador, status e identificador do cliente. O único status disponível é **Confirmada**. Não há cancelamento de venda depois de confirmada.

**Filtrar** atualiza a lista e os totais. **CSV** baixa o relatório do filtro atual. **Anterior** e **Próxima** trocam a página.

Mais abaixo, **Produtos mais vendidos** usa o mesmo período.

O histórico de um cliente também abre daqui. Na tela de clientes, o botão **Vendas** chega neste relatório já filtrado por aquele cliente.

## Conflitos do PDV

Administrador e gerente. Tela **Conflitos de sincronização**.

Uma venda do caixa pode ficar pendente quando o servidor não aceita o preço, o produto, o pagamento ou o estoque. A tela lista essas pendências.

1. Abra o conflito. A venda local aparece com o motivo.
2. Para aceitar, use **Aceitar**. A observação é opcional. O servidor registra a venda com o preço praticado no PDV e baixa o estoque.
3. Para rejeitar, preencha **Observação da decisão** e use **Rejeitar**. Sem essa observação, a rejeição não segue.
4. Use **Atualizar** para reler a lista.

Antes de enviar **Aceitar** ou **Rejeitar**, confirme a decisão no painel do conflito. **Cancelar** fecha essa confirmação sem resolver a venda. A tela preserva a lista durante a atualização e impede o envio repetido da mesma decisão.

Aceitar ou rejeitar encerra a pendência neste admin. O caixa não muda sozinho: no PDV a venda continua marcada como conflito. Avise o operador se a loja precisar tratar o comprovante local.

## Clientes

Administrador e gerente. Tela **Clientes**.

Busque por nome, telefone, e-mail ou documento. Cadastre ou edite nome, telefone, e-mail, documento e se o cliente está ativo. Cliente inativo não deve ser escolhido numa venda nova do PDV.

O botão **Vendas** abre o relatório filtrado por aquele cliente. Não há uma ficha separada de histórico.

## Fornecedores

Administrador e gerente. Tela **Fornecedores**.

Cadastre nome, documento, telefone, e-mail e status. O fornecedor ativo pode ser escolhido na entrada de estoque. O vínculo é opcional: a entrada também aceita ficar sem fornecedor.
