# PDV Desktop

Cliente desktop do PDV Mercado One em JavaFX 25. O PDV e a interface de venda presencial e opera parcialmente offline quando a comunicacao com o servidor estiver indisponivel.

## Estado atual

Implementado:

- Aplicacao JavaFX (`com.mercadoone.pdv.PdvDesktopApplication`).
- Tela unica: URL da API (default `http://localhost:8080`), login (placeholder `operador`), busca de produtos/clientes, carrinho, um pagamento, comprovante textual e feedback.
- `OfflineStorageConfig` com SQLite em `~/.mercado-one/pdv-offline.sqlite3`.
- Fila SQLite local com estados `PENDING`, `SENT`, `CONFLICT` e `ERROR`.
- Catalogo local SQLite para fallback de busca.
- `OnlineSaleClient` para login Bearer, consulta online e `POST /api/offline/sales/sync`.
- `OnlineSaleClient.finalizeSale` monta `POST /api/sales` e e testado; a UI nao chama esse metodo.
- Toda finalizacao grava localmente antes da rede, mesmo com API no ar.
- Login reenvia vendas `PENDING` e `ERROR`. `CONFLICT` nao e reenviado.
- Comprovante simples nao fiscal apos gravar a venda local.
- Testes unitarios de armazenamento local, fila, catalogo e cliente HTTP.

Ainda planejado:

- Badge `SyncStatus` dinamico (o enum existe; o cabecalho fica em `OFFLINE_READY`).
- Refletir no SQLite o aceite/rejeicao feito no admin.
- Mais de um pagamento na UI, desconto e refinamentos de UX.

## Comandos

```bash
mvn javafx:run
mvn test
```

Use um usuario real para entrar. O seed local cria `admin` / `admin123`, nao o placeholder `operador`.

## Documentacao local

- [Offline e sincronizacao](docs/offline-sync.md)

Documentacao transversal relacionada:

- [Offline PDV e sync](../docs/offline-pdv-sync.md)
- [Backlog MVP](../docs/backlog-mvp.md)
- [Testes e verificacao](../docs/testes-e-verificacao.md)
