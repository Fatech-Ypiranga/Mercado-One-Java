# Uso do PDV

Este guia descreve a venda presencial no caixa. A instalação fica em [Desenvolvimento local](desenvolvimento-local.md). O que acontece com a fila e os conflitos, em detalhe técnico, fica em [Offline PDV e sync](offline-pdv-sync.md).

## Quem usa

O operador de caixa usa uma conta cadastrada no admin, em geral com perfil de operador de caixa. O campo **Login** abre preenchido com `operador`; troque pelo login do usuário real. No ambiente de desenvolvimento, a senha do usuário inicial está em [Desenvolvimento local](desenvolvimento-local.md).

A tela também pede a **API**. O valor inicial é `http://localhost:8080`. Só altere se o administrador indicar outro endereço.

## Registrar uma venda

1. Confira o endereço da API, informe login e senha e use **Entrar**.
2. Se a entrada funcionar, a mensagem passa a `Operador autenticado. Busca de produtos liberada.` A senha some do campo.
3. Busque o produto pelo nome, SKU ou código e use **Buscar produtos**.
4. Selecione o produto, informe a quantidade e use **Adicionar item**. A quantidade precisa ser maior que zero.
5. Repita para os outros itens. **Remover ultimo item** tira só o último. **Limpar carrinho** esvazia a venda ainda não finalizada.
6. Se o cliente for identificado, busque em **Buscar clientes** e selecione. Para venda sem cliente, use **Consumidor nao identificado**.
7. Escolha uma forma de pagamento: Dinheiro, Cartão, PIX ou Fiado. A tela registra um pagamento por venda.
8. Confira o total e use **Finalizar venda online**.

O comprovante aparece na hora, em texto, e não é nota fiscal. A mensagem seguinte diz se a venda só ficou no caixa ou se o servidor também aceitou.

## O que a finalização faz

O botão grava a venda neste computador antes de falar com o servidor, mesmo quando a rede está no ar. O carrinho é limpo depois dessa gravação.

| Mensagem | Significado |
| --- | --- |
| `Venda salva localmente e sincronizada com a API.` | O servidor aceitou. |
| Mensagem de conflito, como preço alterado, produto inativo ou estoque | A venda ficou no caixa e virou pendência para o administrador ou o gerente resolverem no admin, na tela **Offline**. |
| `Venda salva localmente.` seguida de falha | A venda ficou no caixa. Na próxima entrada do operador, o PDV tenta enviar de novo. |

Não apague o arquivo local para "desfazer" uma venda. Não há cancelamento depois de finalizada.

## Quando a rede falha na busca

Se a API não responder, a busca de produtos usa a cópia local dos produtos ativos e dos preços que o caixa já tinha sincronizado. **Sincronizar catalogo local** atualiza essa cópia; é preciso estar autenticado.

Cliente identificado depende da API. Sem rede, a busca de clientes não completa.

## O que o caixa não resolve

- Conflito aceito ou rejeitado no admin não muda o estado desta venda no PDV. Ela continua como conflito no caixa.
- Não há cadastro de produto, cliente, estoque, desconto nem segunda forma de pagamento na mesma venda.
- O cabeçalho não troca o selo de sincronização durante o uso. Acompanhe o resultado pela mensagem de texto e pelo comprovante.
