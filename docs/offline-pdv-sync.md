# Offline PDV e Sincronizacao

O PDV offline parcial e uma capacidade implementada em scaffold funcional no MVP. O estado atual registra vendas localmente antes da rede, mantem catalogo local SQLite, sincroniza com a API e preserva conflitos para resolucao administrativa.

## Objetivo

Permitir que o operador registre vendas presenciais mesmo quando a comunicacao com a API estiver temporariamente indisponivel, preservando a venda localmente ate a sincronizacao.

## Estado atual

Implementado:

- `OfflineStorageConfig` define o caminho local `~/.mercado-one/pdv-offline.sqlite3`.
- `SyncStatus` define estados de exibicao iniciais.
- A tela JavaFX mostra status, informa o caminho do armazenamento e finaliza venda persistindo localmente antes da sincronizacao.
- Fila SQLite local com venda, itens, pagamentos e tentativas.
- Catalogo local SQLite com produtos ativos e precos vigentes sincronizados a partir da API.
- Endpoint `POST /api/offline/sales/sync` para sincronizacao de venda local.
- Endpoints administrativos para listar e resolver conflitos de sincronizacao.

Nao implementado ainda:

- Refinamentos de UX para operacao em piloto.

## Estados locais persistidos

O PDV persiste os seguintes estados de venda offline:

- `PENDING`: pendente de sincronizacao.
- `SENT`: sincronizada.
- `CONFLICT`: sincronizada com conflito registrado.
- `ERROR`: falha tecnica de sincronizacao.

O enum `SyncStatus` cobre estados de exibicao da aplicacao. O enum `LocalSaleStatus` cobre o status persistido da venda local.

## Fluxo atual

1. PDV detecta indisponibilidade da API.
2. Operador realiza venda usando catalogo local previamente carregado.
3. PDV grava a venda com identificador local.
4. Venda entra na fila offline.
5. PDV exibe status pendente.
6. Quando a comunicacao retorna, PDV envia vendas pendentes.
7. API aceita a venda ou registra conflito consultavel no admin web.
8. PDV atualiza o status local sem apagar a venda original.
9. Administrador ou gerente aceita ou rejeita o conflito manualmente.

## Conflitos

Conflitos previstos:

- Produto ausente, inativo ou alterado no servidor.
- Preco local diferente do preco vigente.
- Estoque insuficiente no momento da sincronizacao.

Conflito nao deve apagar a venda offline original. A resolucao manual atual permite aceitar a venda com o preco praticado offline, gerando venda e baixa de estoque, ou rejeitar o conflito com observacao.

## Limites do MVP

O modo offline nao precisa permitir:

- Cadastro de produtos.
- Cadastro completo de clientes.
- Cancelamento pos-venda.
- Ajuste de estoque.
- Emissao fiscal.
