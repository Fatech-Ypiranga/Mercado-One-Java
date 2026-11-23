# Uso do PDV

Este guia descreve a venda presencial no caixa. A instalação fica em [Desenvolvimento local](../desenvolvimento/desenvolvimento-local.md). O que acontece com a fila e os conflitos, em detalhe técnico, fica em [Offline PDV e sync](../arquitetura/offline-pdv-sync.md).

## Quem usa

O operador de caixa usa uma conta cadastrada no admin, em geral com perfil de operador de caixa. O PDV abre na **tela de entrada**, com o último login usado neste computador; troque pelo login do usuário real. No ambiente de desenvolvimento, a senha do usuário inicial está em [Desenvolvimento local](../desenvolvimento/desenvolvimento-local.md).

O endereço da **API** fica em **Endereço do servidor**, no rodapé da tela de entrada. O valor inicial é `http://localhost:8080`, ou o endereço da variável `MERCADO_ONE_API_URL`. Só altere se o administrador indicar outro endereço.

## A tela

- **Cabeçalho:** selo de sincronização (`Online`, `Offline pronto`, `Sincronizacao pendente` ou `Falha de sincronizacao`), o operador conectado e o caminho do arquivo local.
- **Coluna da esquerda:** sessão, busca de produtos e cliente.
- **Coluna do meio:** carrinho, desconto, totais e pagamento.
- **Coluna da direita:** comprovante e fila local.
- **Rodapé:** mensagem do último passo.

## Registrar uma venda

1. Na tela de entrada, informe login e senha e use **Entrar** (ou Enter no campo da senha). A tela de venda só abre depois disso. Com a API no ar, a mensagem passa a `Operador autenticado. Atualizando catalogo e fila offline...`, e o PDV atualiza o catálogo local e envia o que estiver na fila. Sem a API, veja [Entrada sem internet](#entrada-sem-internet).
2. Leia o código de barras com o leitor (veja [Leitor de código de barras](#leitor-de-código-de-barras)): o item entra sozinho no carrinho. Sem leitor, busque o produto por código de barras, SKU ou nome e use **Buscar**.
3. Na busca manual, selecione o produto, informe a quantidade e use **Adicionar item**. A quantidade precisa ser maior que zero.
4. No carrinho, **Remover item selecionado** tira um item e **Alterar quantidade** troca a quantidade do item selecionado. **Cancelar venda em andamento** esvazia a venda ainda não finalizada; nada é registrado.
5. Se o cliente for identificado, busque por nome, telefone ou documento e selecione. Para venda sem cliente, use **Consumidor nao identificado**. Cliente novo pode ser cadastrado na hora em **Cadastro rapido** (nome, telefone e se aceita contato), com a API disponível.
6. Se o perfil permitir, informe o **Desconto (R$)**. A dica abaixo do campo mostra o limite do perfil. O padrão do operador de caixa é sem desconto.
7. Registre os pagamentos: escolha a forma (Dinheiro, Cartao, PIX ou Fiado), informe o valor e use **Adicionar pagamento**. Dá para combinar formas na mesma venda. A linha de totais mostra subtotal, desconto, total e quanto ainda falta.
8. Use **Finalizar venda**. Sem nenhum pagamento lançado, o PDV usa a forma selecionada pelo valor do campo, que já vem com o total.

A soma dos pagamentos tem de ser igual ao total. Se faltar ou sobrar, a tela avisa e não finaliza.

O comprovante aparece em texto e não é nota fiscal.

## Leitor de código de barras

O PDV funciona com leitor de código de barras USB em **modo teclado**, que é o modo de fábrica da maioria dos modelos (inclusive o Tomate 1D USB comprado para o projeto, mas faça o teste abaixo). O leitor "digita" o código e termina com Enter, sem driver e sem configuração no PDV.

**Como funciona**

- Leu um código que bate exatamente com **um** produto (código de barras ou SKU): o item entra sozinho no carrinho, com a mensagem `<produto> adicionado.`, e o campo **Buscar** fica livre e com o foco para a próxima leitura.
- Leu o mesmo produto de novo: a quantidade da linha aumenta (três leituras dão uma linha de quantidade 3), em vez de criar linhas repetidas.
- Para vender várias unidades de uma vez, digite a quantidade no campo **Quantidade** antes de ler. Ela volta a 1 depois de cada item.
- Código que não existe no cadastro: o PDV avisa `Codigo <numero> nao encontrado.`, não altera o carrinho e deixa o número selecionado.
- Busca por **nome** não adiciona sozinha: lista os produtos e o operador escolhe.
- Funciona também **sem internet**, pelo catálogo local. Por isso, mantenha o catálogo atualizado (**Atualizar catalogo local**) depois de cadastrar produtos novos.
- Se o foco estiver em outro lugar da tela (carrinho, botão), a leitura é desviada para o campo **Buscar**. Campos de texto, como login, cliente e desconto, não são afetados: o que o leitor digitar neles fica neles.
- O Enter solto não aciona mais o botão **Entrar**: o login só é acionado pelo Enter dentro dos campos de login e senha.

**Teste de bancada (10 segundos, antes de usar no caixa)**

1. Ligue o leitor **direto numa porta USB do computador**, não num hub sem alimentação própria (o Tomate consome até 400 mA).
2. Abra o **Bloco de Notas** e leia qualquer código de barras.
3. Os números devem aparecer e o cursor deve pular para a linha de baixo. Se aconteceu, está no modo certo.

**Se algo não sair como esperado**

| O que acontece | O que fazer |
| --- | --- |
| Nada aparece no Bloco de Notas | Troque a porta USB e o cabo. Confira se a luz vermelha acende ao apertar o gatilho. Se acender mas nada for digitado, o leitor pode estar em modo serial (porta COM): leia o código de configuração "modo teclado / USB HID" do manual do fabricante |
| Os números aparecem, mas o cursor não pula de linha | O leitor está sem o sufixo Enter. Leia o código de configuração "adicionar Enter/CR no final" do manual. Sem ele, o PDV não busca sozinho: use **Buscar** |
| Aparecem letras trocadas ou símbolos no lugar de números | Layout de teclado do Windows diferente do esperado. Códigos só de dígitos (EAN-13) não sofrem com isso; confira o idioma do teclado nas configurações do Windows |
| O mesmo produto é lido duas vezes | Desligue o modo de leitura contínua ou reduza a repetição do leitor, conforme o manual |
| `Codigo ... nao encontrado` com o produto cadastrado | Confira o código de barras no cadastro (admin web, tela **Produtos**) e atualize o catálogo local. O produto precisa estar **ativo** |
| QR Code não lê | Esperado: o Tomate 1D lê só código de barras linear. QR Code exigiria um leitor 2D |

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

**Sair** (no cabeçalho) volta para a tela de entrada. Se houver itens no carrinho, o PDV pergunta antes, porque a venda em andamento é descartada: o próximo operador não herda o carrinho de quem saiu. As vendas já finalizadas ficam na fila e seguem no próximo login.

## Entrada sem internet

O caixa não pode parar quando a internet cai. Por isso a entrada funciona em dois modos:

| Situação | O que acontece |
| --- | --- |
| API no ar | O servidor confere login e senha. Se estiverem certos, o operador entra e o PDV guarda neste computador uma credencial local (veja abaixo). |
| API fora do ar, operador **já entrou online neste computador** | A senha é conferida contra a credencial local. O operador entra com o aviso `Entrou sem conexão...` e `(sem conexão)` ao lado do nome. As vendas vão para a fila. |
| API fora do ar, operador **nunca entrou online neste computador** | Recusado: `Sem conexão com o sistema. Sem internet só entra quem já entrou neste caixa com internet.` |
| API no ar e **senha errada** | Recusado, sem plano B. Senha errada é senha errada, com ou sem internet. |

- **A credencial local não guarda a senha.** Guarda um hash com sal (PBKDF2-HMAC-SHA256, 210 mil iterações) em `~/.mercado-one/pdv-offline.sqlite3`. Ela é renovada a cada entrada online, então uma senha trocada no admin passa a valer offline na próxima vez que o operador entrar com internet.
- Quem entrou offline mantém a senha digitada só na memória da sessão. A cada 2 minutos, e em **Sincronizar agora**, o PDV tenta obter o acesso à API com ela. Quando consegue, envia a fila sozinho. **Sair** apaga a senha da memória.
- **Cada venda guarda quem vendeu.** O PDV grava o login do operador na venda local e o envia à API. A API credita a venda a esse operador, mesmo que outra pessoa entre depois e sincronize a fila. O limite de desconto também segue o perfil de quem vendeu.
- Se o login gravado na venda não existir ou estiver inativo no servidor, a venda vira conflito `OPERATOR_NOT_FOUND` e o gerente decide na tela **Offline** do admin.
- Sem internet não há desconto: o limite do perfil vem da API. A dica no campo mostra `Sem conexão: o desconto só vale depois que o sistema voltar.`
- Esqueci a senha: o sistema não envia e-mail. O administrador redefine em **Usuários, Redefinir senha** e a pessoa troca a senha em **Minha conta**. A nova senha vale offline depois da próxima entrada com internet.

## O que o caixa não faz

- Cadastro de produto, ajuste de estoque e cancelamento de venda confirmada.
- Emissão fiscal.
