# Uso do PDV

Este guia descreve a venda presencial no caixa. A instalação fica em [Desenvolvimento local](desenvolvimento-local.md). O que acontece com a fila e os conflitos, em detalhe técnico, fica em [Offline PDV e sync](offline-pdv-sync.md).

## Quem usa

O operador de caixa usa uma conta cadastrada no admin, em geral com perfil de operador de caixa. O campo **Login** abre preenchido com `operador`; troque pelo login do usuário real. No ambiente de desenvolvimento, a senha do usuário inicial está em [Desenvolvimento local](desenvolvimento-local.md).

A tela também pede a **API**. O valor inicial é `http://localhost:8080`, ou o endereço da variável `MERCADO_ONE_API_URL`. Só altere se o administrador indicar outro endereço.

## A tela

- **Cabeçalho:** selo de sincronização (`Online`, `Offline pronto`, `Sincronizacao pendente` ou `Falha de sincronizacao`), o operador conectado e o caminho do arquivo local.
- **Coluna da esquerda:** sessão, busca de produtos e cliente.
- **Coluna do meio:** carrinho, desconto, totais e pagamento.
- **Coluna da direita:** comprovante e fila local.
- **Rodapé:** mensagem do último passo.

## Registrar uma venda

1. Confira o endereço da API, informe login e senha e use **Entrar**. A mensagem passa a `Operador autenticado. Atualizando catalogo e fila offline...`, a senha some do campo e o PDV atualiza o catálogo local e envia o que estiver na fila.
2. Busque o produto por código de barras, SKU ou nome e use **Buscar**.
3. Selecione o produto, informe a quantidade e use **Adicionar item**. A quantidade precisa ser maior que zero.
4. No carrinho, **Remover item selecionado** tira um item e **Alterar quantidade** troca a quantidade do item selecionado. **Cancelar venda em andamento** esvazia a venda ainda não finalizada; nada é registrado.
5. Se o cliente for identificado, busque por nome, telefone ou documento e selecione. Para venda sem cliente, use **Consumidor nao identificado**. Cliente novo pode ser cadastrado na hora em **Cadastro rapido** (nome, telefone e se aceita contato), com a API disponível.
6. Se o perfil permitir, informe o **Desconto (R$)**. A dica abaixo do campo mostra o limite do perfil. O padrão do operador de caixa é sem desconto.
7. Registre os pagamentos: escolha a forma (Dinheiro, Cartao, PIX ou Fiado), informe o valor e use **Adicionar pagamento**. Dá para combinar formas na mesma venda. A linha de totais mostra subtotal, desconto, total e quanto ainda falta.
8. Use **Finalizar venda**. Sem nenhum pagamento lançado, o PDV usa a forma selecionada pelo valor do campo, que já vem com o total.

A soma dos pagamentos tem de ser igual ao total. Se faltar ou sobrar, a tela avisa e não finaliza.

O comprovante aparece em texto e não é nota fiscal.

## O que a finalização faz

O botão grava a venda neste computador antes de falar com o servidor, mesmo quando a rede está no ar. Depois, com operador conectado, envia a venda à API.

| Mensagem | Significado |
| --- | --- |
| `Venda confirmada na API (#N).` | O servidor aceitou com o preço vigente. |
| `API indisponivel. Venda salva na fila offline e sera reenviada.` | A venda ficou no caixa e vai na próxima sincronização. |
| `Sessao expirada. Venda salva na fila offline; entre novamente para enviar.` | Entre de novo para enviar. |
| `Sem sessao na API. Venda salva na fila offline e sera enviada no proximo login.` | Venda feita sem operador conectado. |
| `Venda recusada pela API: ...` | Regra de negócio recusou (por exemplo, estoque insuficiente). A venda fica como recusada no caixa. |

Não apague o arquivo local para "desfazer" uma venda. O cancelamento de venda confirmada é feito pelo administrador ou gerente no admin.

## Fila local e sincronização

A sincronização roda no login, a cada 2 minutos e no botão **Sincronizar agora**. Ela reenvia as vendas pendentes ou com falha e consulta as decisões do admin sobre conflitos.

Na **Fila local**, cada venda aparece como:

- `Pendente`: ainda não enviada.
- `Sincronizada #N`: aceita pelo servidor.
- `Conflito (aguarda admin)`: o servidor encontrou diferença de preço, produto, estoque, desconto ou pagamento. O administrador ou o gerente decide na tela **Offline** do admin.
- `Falha, sera reenviada`: erro de rede ou servidor; vai de novo na próxima sincronização.
- `Recusada`: recusada no envio ou rejeitada no admin. Não é reenviada.

Quando o admin aceita ou rejeita um conflito, a venda muda para `Sincronizada` ou `Recusada` na próxima sincronização.

## Quando a rede falha

Se a API não responder, a busca de produtos usa a cópia local dos produtos ativos e dos preços já sincronizados. **Atualizar catalogo local** renova essa cópia; é preciso estar conectado.

Busca e cadastro de cliente dependem da API. Sem rede, venda sem cliente.

**Sair** encerra a sessão; as vendas seguintes vão para a fila até o próximo login.

## O que o caixa não faz

- Cadastro de produto, ajuste de estoque e cancelamento de venda confirmada.
- Emissão fiscal.
