# Offline e Sincronizacao do PDV

Este documento registra a visao local do PDV desktop para a operacao offline parcial.

## Estado atual

O PDV registra vendas em fila local antes de tentar sincronizar e mantem um catalogo local para venda durante indisponibilidade temporaria. A base existente inclui:

- Uma aplicacao JavaFX executavel.
- Um status inicial `OFFLINE_READY`.
- O caminho de armazenamento local em `~/.mercado-one/pdv-offline.sqlite3`.
- Tabelas SQLite para vendas, itens, pagamentos e tentativas.
- Tabela SQLite para catalogo local de produtos ativos e precos vigentes.
- Um cliente HTTP para login, busca online e sincronizacao de venda offline contra a API.
- Estados persistidos `PENDING`, `SENT`, `CONFLICT` e `ERROR`.
- Busca de produto com fallback local quando a API nao responde.
- Comprovante simples nao fiscal exibido apos a finalizacao da venda.

## Responsabilidades atuais e futuras do PDV

- Registrar venda offline com identificador local antes de qualquer chamada de rede.
- Preservar venda na fila local ate confirmacao do servidor.
- Exibir status de sincronizacao de forma clara para o operador.
- Reenviar vendas pendentes ou com erro quando a comunicacao retornar.
- Manter falhas e conflitos visiveis para revisao no admin web.
- Evoluir mensagens e UX para operacao piloto sem reduzir a garantia de preservacao local.

## O que nao pertence ao PDV offline no MVP

- Cadastro completo de produtos.
- Cadastro completo de clientes.
- Ajuste de estoque.
- Cancelamento pos-venda.
- Emissao fiscal.

## Cuidado principal

O PDV nao deve perder venda registrada localmente. Falhas de rede, conflitos ou erros de servidor devem preservar o registro original e permitir nova tentativa ou revisao.
