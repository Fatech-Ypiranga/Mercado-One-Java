# PDV Desktop

Cliente desktop do PDV Mercado One em JavaFX. O PDV e a interface de venda presencial e opera parcialmente offline quando a comunicacao com o servidor estiver indisponivel.

## Estado atual

Implementado:

- Aplicacao JavaFX em Java 25.
- Tela inicial com cabecalho, marca, status de sincronizacao, login de operador, busca online de produtos/clientes, carrinho e venda preservada em fila local antes de sincronizar.
- Enum `SyncStatus` com estados iniciais de exibicao.
- `OfflineStorageConfig` com caminho padrao para SQLite local em `~/.mercado-one/pdv-offline.sqlite3`.
- Fila SQLite local com estados `PENDING`, `SENT`, `CONFLICT` e `ERROR`.
- Catalogo local SQLite com produtos ativos e precos vigentes para fallback de busca e venda offline.
- `OnlineSaleClient` para login, consulta online e sincronizacao de venda para `POST /api/offline/sales/sync`.
- Dependencia `sqlite-jdbc`.
- Comprovante simples nao fiscal exibido apos finalizar venda.
- Testes unitarios do armazenamento local, fila offline, catalogo local e cliente HTTP.

Ainda planejado:

- Refinamentos de UX para operacao piloto e comunicacao de conflitos ao operador.

## Comandos

```bash
mvn javafx:run
mvn test
```

## Documentacao local

- [Offline e sincronizacao](docs/offline-sync.md)

Documentacao transversal relacionada:

- [Offline PDV e sync](../docs/offline-pdv-sync.md)
- [Backlog MVP](../docs/backlog-mvp.md)
- [Testes e verificacao](../docs/testes-e-verificacao.md)
