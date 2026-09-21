# Offline e Sincronizacao do PDV

Este documento registra a visao local do PDV desktop para a operacao offline parcial.

## Estado atual

O PDV registra vendas em fila local antes de tentar sincronizar e mantem um catalogo local para venda durante indisponibilidade temporaria. A base existente inclui:

- Aplicacao JavaFX executavel.
- Enum `SyncStatus` com rotulos `ONLINE`, `OFFLINE_READY`, `SYNC_PENDING` e `SYNC_FAILED`. O cabecalho da tela inicializa em `OFFLINE_READY` e nao atualiza esse badge.
- Caminho `~/.mercado-one/pdv-offline.sqlite3`.
- Tabelas SQLite `offline_sales`, `offline_sale_items`, `offline_sale_payments`, `offline_sale_sync_attempts` e `local_catalog_products`.
- Cliente HTTP para login, busca online e `POST /api/offline/sales/sync`.
- Estados persistidos `PENDING`, `SENT`, `CONFLICT` e `ERROR`.
- Reenvio no login de `PENDING` e `ERROR`.
- Busca de produto com fallback local quando a API nao responde ou nao ha token.
- Comprovante simples nao fiscal apos gravar a venda local, antes da resposta da API.

A UI nao chama `POST /api/sales`. Resolucao de conflito no admin nao altera o status local.

## Responsabilidades atuais e futuras do PDV

- Registrar venda com identificador local antes de qualquer chamada de rede.
- Preservar venda na fila local ate confirmacao do servidor ou revisao de conflito.
- Comunicar resultado de sync ao operador (hoje via texto de feedback, nao via badge).
- Reenviar vendas `PENDING`/`ERROR` quando a comunicacao retornar.
- Manter falhas e conflitos visiveis para revisao no admin web.
- Evoluir mensagens e UX para piloto sem reduzir a garantia de preservacao local.

## O que nao pertence ao PDV offline no MVP

- Cadastro completo de produtos.
- Cadastro completo de clientes.
- Ajuste de estoque.
- Cancelamento pos-venda.
- Emissao fiscal.
- Desconto.

## Cuidado principal

O PDV nao deve perder venda registrada localmente. Falhas de rede, conflitos ou erros de servidor devem preservar o registro original e permitir nova tentativa ou revisao.
