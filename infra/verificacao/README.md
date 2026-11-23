# Verificação integrada do MVP

Os testes automatizados do backend usam H2 e MockMvc, e os do admin web usam a API simulada. Este roteiro prova os critérios de aceite do MVP contra a **API real com PostgreSQL**, como a loja usaria.

`smoke-mvp.mjs` (Node 18+, sem dependências) exercita, em 71 verificações: login e permissões por perfil, usuário inativo, produto, categoria, fornecedor e cliente, entrada e ajuste de estoque, venda online (preço do servidor, pagamentos múltiplos, desconto, fiado, idempotência, estoque insuficiente), cancelamento, venda offline com sincronização e conflito de preço (aceite e recusa), histórico do cliente, relatórios, auditoria e troca de senha.

## Cuidado

O roteiro **cria dados de teste** (usuários, produtos, vendas). Nunca aponte para o banco de desenvolvimento ou de produção: use um PostgreSQL descartável.

## Como rodar (PowerShell, na raiz do repositório)

```powershell
# 1. Banco descartável, em porta própria
docker run -d --name mercado-one-smoke-pg -e POSTGRES_DB=mercado_one_smoke -e POSTGRES_USER=smoke -e POSTGRES_PASSWORD=smoke -p 55432:5432 postgres:16-alpine

# 2. API apontando para ele. Variáveis do processo têm prioridade sobre o .env
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
$env:MERCADO_ONE_DATABASE_URL = 'jdbc:postgresql://localhost:55432/mercado_one_smoke'
$env:MERCADO_ONE_DB_USER = 'smoke'
$env:MERCADO_ONE_DB_PASSWORD = 'smoke'
$env:MERCADO_ONE_API_PORT = '8085'
$env:PORT = '8085'
$env:MERCADO_ONE_JWT_SECRET = 'segredo-de-teste-0123456789-abcdefghijklmnopqrstuvwxyz'
$env:MERCADO_ONE_SEED_ADMIN_ENABLED = 'true'
$env:MERCADO_ONE_SEED_ADMIN_NAME = 'Administrador'
$env:MERCADO_ONE_SEED_ADMIN_LOGIN = 'admin'
$env:MERCADO_ONE_SEED_ADMIN_PASSWORD = 'admin123'
cd backend-api
mvn spring-boot:run          # deixe rodando e abra outro terminal

# 3. Roteiro (outro terminal, na raiz)
node infra/verificacao/smoke-mvp.mjs http://localhost:8085

# 4. Limpeza
docker rm -f mercado-one-smoke-pg
```

Saída esperada no fim: `==== 71 passaram, 0 falharam ====`. Em falha, o roteiro lista cada verificação que não passou. Pode ser repetido sem recriar o banco, pois os dados usam um sufixo único por execução.

## O que fica de fora

- **Admin web com a API real:** abra o admin em `http://localhost:4200` (não `127.0.0.1`: o cookie é `SameSite=Lax`) e, se a API de teste estiver em outra porta, redirecione as chamadas de `:8080` para ela.
- **PDV com a API real:** `CheckoutService` e `OfflineSaleQueue` podem ser usados contra a API de teste com um SQLite temporário (`OfflineStorageConfig(Path)`). Nunca use o banco local do caixa, `~/.mercado-one/pdv-offline.sqlite3`.
- Interface do PDV (JavaFX) e emissão fiscal, que não fazem parte do MVP.
