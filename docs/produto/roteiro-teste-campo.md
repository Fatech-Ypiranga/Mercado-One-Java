# Roteiro de teste de campo

Para o primeiro dia do piloto, com o sistema no mercado de verdade. Leva cerca de 2 horas. Quem acompanha leva este roteiro impresso e uma caneta: o objetivo é **descobrir o que atrapalha o caixa**, não provar que tudo funciona.

Regra de ouro: se algo travar a venda, **o cliente vem primeiro**. Anote o que aconteceu e resolva depois.

## Antes de começar (30 minutos)

- [ ] API no ar: abrir `/actuator/health` no navegador e ver `UP`.
- [ ] Painel aberto no navegador, com o administrador entrando sem erro.
- [ ] Pelo menos 3 usuários: o administrador, um gerente e **dois operadores de caixa**, cada um com e-mail cadastrado em **Usuários** (se o envio de e-mail estiver ligado).
- [ ] Pelo menos 10 produtos reais cadastrados, com **código de barras**, preço e estoque inicial conferido na prateleira.
- [ ] PDV instalado no computador do caixa, apontando para a API certa (**Endereço do servidor** na tela de entrada).
- [ ] Leitor de código de barras ligado. Teste no Bloco de Notas: ler um produto deve digitar o número e pular de linha.
- [ ] **Cada operador entra uma vez com internet** neste caixa. Sem isso, ele não entra quando a internet cair.
- [ ] Anotar o horário e o estoque de 3 produtos, para conferir no fim.

## O teste, em ordem

Para cada passo, marque o resultado e anote qualquer surpresa na tabela do fim.

| # | O que fazer | Deve acontecer |
| --- | --- | --- |
| 1 | No PDV, entrar com o operador 1 (com internet) | Abre a tela de venda. O nome aparece no topo |
| 2 | Ler 3 produtos com o leitor | Cada leitura entra no carrinho sozinha, sem tocar no mouse |
| 3 | Ler um código que não existe no cadastro | Aviso de código não encontrado, carrinho intacto |
| 4 | Finalizar com um pagamento em dinheiro | Comprovante aparece, carrinho zera, a venda aparece como sincronizada |
| 5 | Vender de novo, pagando metade em dinheiro e metade no Pix | Aceita os dois pagamentos |
| 6 | No painel, abrir **Vendas** e cancelar a venda do passo 5 | Estoque do produto volta |
| 7 | **Desligar a internet do caixa.** Sair, e entrar de novo com o operador 1 | Entra com o aviso de sem conexão |
| 8 | Tentar entrar com o operador 2, que não entrou antes neste caixa | Recusado com explicação clara (se ele já entrou no preparo, entra normalmente) |
| 9 | Vender 3 vendas sem internet | Todas ficam na fila, o caixa não trava |
| 10 | **Religar a internet.** Esperar 2 minutos ou usar **Sincronizar agora** | Fila sincroniza sozinha. No painel, as 3 vendas aparecem em nome do operador 1 |
| 11 | No painel, mudar o preço de um produto. No PDV, **sem internet**, vender esse produto ao preço antigo e religar | A venda vira conflito. Em **Offline**, o gerente aceita e a venda entra |
| 12 | Fazer uma venda com **Sair** no meio, com itens no carrinho | O PDV pergunta antes de descartar |
| 13 | Com e-mail ligado: **Esqueci a senha** com o login do operador 2 | Chega o e-mail, o link funciona uma vez, a senha nova entra |
| 14 | Pedir ao gerente para fazer um desconto que o perfil não permite | Recusado com a explicação do limite |

## No fim do dia (20 minutos)

- [ ] Contar fisicamente os 3 produtos anotados e comparar com o estoque do painel. **A diferença deve ser exatamente as vendas feitas.**
- [ ] Em **Meu caixa**, conferir o total do dia contra o dinheiro e os comprovantes.
- [ ] Em **Offline**, não pode sobrar venda pendente nem conflito aberto.
- [ ] Perguntar ao operador: o que foi mais lento, o que confundiu, o que você tentou fazer e o sistema não deixou.

## O que anotar

Uma linha por surpresa:

| Passo | O que esperava | O que aconteceu | Atrapalhou a venda? (nenhum / pouco / parou) |
| --- | --- | --- | --- |
|  |  |  |  |
|  |  |  |  |
|  |  |  |  |

## Como decidir

- **Passou**: nenhum "parou", estoque bate, fila zerada no fim.
- **Ajustar antes de seguir**: qualquer "parou", ou estoque que não bate. Anote a hora e o passo exato.
- **Não dá para ligar para o cliente**: voltar ao método antigo no caixa e registrar a venda depois no sistema.

Detalhes de cada tela estão em [Uso do PDV](uso-pdv.md) e [Uso do admin web](uso-admin-web.md). O que fazer quando algo falha: [Offline PDV e sync](../arquitetura/offline-pdv-sync.md).
