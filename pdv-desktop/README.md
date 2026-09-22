# PDV Desktop

Cliente desktop do PDV Mercado One em JavaFX 25. Venda presencial com fila local em SQLite.

Uso do caixa: [Uso do PDV](../docs/uso-pdv.md). Fila, sync e conflitos: [Offline PDV e sync](../docs/offline-pdv-sync.md).

A UI nao chama `POST /api/sales`. Toda finalizacao grava em SQLite e depois tenta `POST /api/offline/sales/sync`. O campo de login vem preenchido com `operador`; o seed local cria `admin` / `admin123`. A URL default da API e `http://localhost:8080`. O SQLite fica em `~/.mercado-one/pdv-offline.sqlite3`.

## Comandos

```bash
mvn javafx:run
mvn test
```

## Documentacao relacionada

- [Offline PDV e sync](../docs/offline-pdv-sync.md)
- [Dados e migracoes](../docs/dados-e-migracoes.md)
- [Testes e verificacao](../docs/testes-e-verificacao.md)
