# Uso do admin web

Este guia descreve o que cada pessoa da loja faz no administrativo. A instalação do ambiente fica em [Desenvolvimento local](desenvolvimento-local.md). O que o sistema ainda não faz fica em [Backlog MVP](backlog-mvp.md).

## Quem entra

O menu lateral só mostra o que o perfil pode abrir.

| Perfil | O que vê |
| --- | --- |
| Administrador | Início, meu caixa, vendas, conflitos offline, produtos, categorias, estoque, fornecedores, clientes, usuários, auditoria e minha conta |
| Gerente | O mesmo, exceto usuários e auditoria |
| Estoquista | Início, estoque e minha conta |
| Operador de caixa | Meu caixa e minha conta |

A venda presencial fica no [PDV](uso-pdv.md). O operador de caixa entra direto em **Meu caixa**.

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

Para trocar a senha de alguém, use **Redefinir senha** na linha do usuário e informe a nova senha (mínimo 6 caracteres).

Um usuário inativo não consegue entrar. Depois de qualquer alteração no usuário (senha, perfil ou status), as sessões abertas dele deixam de valer e ele precisa entrar de novo. O operador de caixa criado aqui vende no PDV.

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

1. Escolha **Entrada de um produto**, **Nota de entrada (vários produtos)** ou **Ajuste manual**.
2. Na entrada de um produto, selecione um produto ativo, informe a quantidade e, se houver, o fornecedor e o documento.
3. Na nota de entrada, informe fornecedor e documento uma vez e monte os **Itens da nota** com **Adicionar item**. A nota aceita até 200 produtos.
4. No ajuste, selecione o produto, informe o novo saldo e a justificativa. Sem justificativa o formulário não segue.
5. Use **Registrar**.

Os saldos, o formulário **Registrar movimentação** e o **Histórico de movimentações** ficam em seções separadas. A lista e o histórico continuam visíveis durante uma atualização, com indicação de que os dados estão sendo atualizados.

O saldo sobe na entrada confirmada e volta quando uma venda é cancelada. O ajuste substitui o saldo pelo valor informado e exige o motivo. Cada alteração entra no histórico, que pode ser filtrado por tipo: entradas, ajustes, vendas e cancelamentos de venda.

## Vendas

Administrador e gerente. Tela **Relatório de vendas**.

Filtre por período, identificador do operador, status e identificador do cliente. Status: **Confirmada** ou **Cancelada**. Os totais mostram também descontos e canceladas.

Para cancelar uma venda confirmada, use **Cancelar venda**, informe o motivo e confirme. Sem motivo o cancelamento não segue. O estoque dos itens é devolvido e o cancelamento entra na auditoria.

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

Aceitar ou rejeitar encerra a pendência neste admin. Na próxima sincronização do PDV (no login, a cada 2 minutos ou em **Sincronizar agora**), a venda muda no caixa para sincronizada ou recusada. Ao aceitar, o estoque é baixado mesmo que fique negativo, porque a mercadoria já saiu da loja.

## Clientes

Administrador e gerente. Tela **Clientes**.

Busque por nome, telefone, e-mail ou documento. Cadastre ou edite nome, telefone, e-mail, documento e se o cliente está ativo. Cliente inativo não deve ser escolhido numa venda nova do PDV.

O botão **Vendas** abre o relatório filtrado por aquele cliente. Não há uma ficha separada de histórico.

## Fornecedores

Administrador e gerente. Tela **Fornecedores**.

Cadastre nome, documento, telefone, e-mail e status. O fornecedor ativo pode ser escolhido na entrada de estoque. O vínculo é opcional: a entrada também aceita ficar sem fornecedor.

## Meu caixa

Operador de caixa, gerente e administrador. Tela **Meu caixa**.

Mostra só as vendas do próprio usuário no dia escolhido: quantidade de vendas, total, descontos, canceladas e totais por forma de pagamento, além da lista **Vendas do dia**. Use **Atualizar** para reler.

## Auditoria

Somente o administrador. Tela **Auditoria**.

Lista os eventos registrados pelo sistema, do mais recente para o mais antigo: vendas, cancelamentos, movimentos e notas de estoque, produtos, usuários e vendas offline. Filtre por ação, tipo de registro, identificador do registro, identificador do usuário e período e use **Filtrar**. **Detalhes** mostra o resumo gravado no evento. **Anterior** e **Próxima** trocam a página.

## Minha conta

Todos os perfis. Tela **Minha conta**.

Para **Alterar senha**, informe a senha atual, a nova senha (mínimo 6 caracteres) e a confirmação. A nova senha vale para o admin e para o PDV. A sessão atual deixa de valer e o próximo acesso pede login de novo.
