# Uso do PDV

Este guia descreve a venda presencial no caixa. A instalacao fica em [Desenvolvimento local](desenvolvimento-local.md). O que acontece com a fila e os conflitos, em detalhe tecnico, fica em [Offline PDV e sync](offline-pdv-sync.md).

## Quem usa

O operador de caixa. O login e um usuario cadastrado no admin, em geral com perfil de operador de caixa. O campo **Login** abre preenchido com `operador`; troque pelo usuario real. No ambiente de desenvolvimento a senha do usuario inicial esta em [Desenvolvimento local](desenvolvimento-local.md).

A tela tambem pede a **API**. O valor inicial e `http://localhost:8080`. So altere se o administrador indicar outro endereco.

## Registrar uma venda

1. Confira o endereco da API, informe login e senha e use **Entrar**.
2. Se a entrada funcionar, a mensagem passa a `Operador autenticado. Busca de produtos liberada.` A senha some do campo.
3. Busque o produto pelo nome, SKU ou codigo e use **Buscar produtos**.
4. Selecione o produto, informe a quantidade e use **Adicionar item**. A quantidade precisa ser maior que zero.
5. Repita para os outros itens. **Remover ultimo item** tira so o ultimo. **Limpar carrinho** esvazia a venda ainda nao finalizada.
6. Se o cliente for identificado, busque em **Buscar clientes** e selecione. Para venda sem cliente, use **Consumidor nao identificado**.
7. Escolha uma forma de pagamento: Dinheiro, Cartao, PIX ou Fiado. A tela registra um pagamento por venda.
8. Confira o total e use **Finalizar venda online**.

O comprovante aparece na hora, em texto, e nao e nota fiscal. A mensagem seguinte diz se a venda so ficou no caixa ou se o servidor tambem aceitou.

## O que a finalizacao faz

O botao grava a venda neste computador antes de falar com o servidor, mesmo quando a rede esta no ar. O carrinho e limpo depois dessa gravacao.

| Mensagem | Significado |
| --- | --- |
| `Venda salva localmente e sincronizada com a API.` | O servidor aceitou. |
| Mensagem de conflito, como preco alterado, produto inativo ou estoque | A venda ficou no caixa e virou pendencia para o administrador ou o gerente resolverem no admin, na tela **Offline**. |
| `Venda salva localmente.` seguida de falha | A venda ficou no caixa. Na proxima entrada do operador, o PDV tenta enviar de novo. |

Nao apague o arquivo local para "desfazer" uma venda. Nao ha cancelamento depois de finalizada.

## Quando a rede falha na busca

Se a API nao responder, a busca de produtos usa a copia local dos produtos ativos e dos precos que o caixa ja tinha sincronizado. **Sincronizar catalogo local** atualiza essa copia; e preciso estar autenticado.

Cliente identificado depende da API. Sem rede, a busca de clientes nao completa.

## O que o caixa nao resolve

- Conflito aceito ou rejeitado no admin nao muda o estado desta venda no PDV. Ela continua como conflito no caixa.
- Nao ha cadastro de produto, cliente, estoque, desconto nem segunda forma de pagamento na mesma venda.
- O cabecalho nao troca o selo de sincronizacao durante o uso. O retorno vale a mensagem de texto e o comprovante.
