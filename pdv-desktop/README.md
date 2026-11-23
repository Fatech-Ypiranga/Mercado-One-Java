# PDV Desktop

Cliente desktop do PDV Mercado One em JavaFX 25. Venda presencial com fila local em SQLite.

Uso do caixa: [Uso do PDV](../docs/produto/uso-pdv.md). Fila, sync e conflitos: [Offline PDV e sync](../docs/arquitetura/offline-pdv-sync.md).

O PDV confirma online por `POST /api/sales` (idempotente por `clientSaleId`) depois de gravar a venda no SQLite; sem API, a venda fica na fila e sincroniza por `POST /api/offline/sales/sync`. O PDV abre na tela de entrada (com o último login usado neste computador) e só mostra a venda depois de aceitar o operador, online ou offline; o seed local cria `admin` / `admin123`. A URL padrão da API é `http://localhost:8080`. O SQLite fica em `~/.mercado-one/pdv-offline.sqlite3`.

## Comandos

```bash
mvn javafx:run
mvn test
```

## Visual

Cores e medidas seguem a [identidade visual](../docs/marca/identidade-visual.html). `src/main/resources/styles/brand-tokens.css` (variáveis `-mo-*`) e `brand-tokens-dark.css` são gerados de `docs/marca/tokens.json` por `node docs/marca/gerar-tokens.mjs` (na raiz) e não devem ser editados à mão. `styles/pdv.css` usa só essas variáveis. O botão **Modo escuro / Modo claro** do cabeçalho aplica as variáveis do modo escuro no nó raiz e guarda a escolha nas preferências do usuário do Windows. A fonte é a Outfit, carregada de `src/main/resources/fonts` (licença OFL em `OFL-Outfit.txt`).

## Documentação relacionada

- [Offline PDV e sync](../docs/arquitetura/offline-pdv-sync.md)
- [Dados e migrações](../docs/dados/dados-e-migracoes.md)
- [Testes e verificação](../docs/desenvolvimento/testes-e-verificacao.md)
