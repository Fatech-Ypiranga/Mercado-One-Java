# Testes e Verificacao

Este documento registra os checks esperados do Mercado One e o estado conhecido da suite atual.

## Checks esperados

Backend:

```bash
cd backend-api
mvn test
```

Admin web:

```bash
cd admin-web
npm test -- --watch=false
```

PDV desktop:

```bash
cd pdv-desktop
mvn test
```

## Testes existentes

- Backend: teste de contexto da aplicacao com H2 e Flyway desabilitado.
- Backend: testes de login, `/api/auth/me` e protecao de rota sem token.
- Backend: testes de perfis, criacao, atualizacao, filtros, validacao de usuarios, bloqueio de operador em gestao de usuarios e revalidacao de role atual para token antigo.
- Backend: testes de criacao, atualizacao e filtros de categorias e produtos.
- Backend: testes de entrada, ajuste, validacao, filtros principais de estoque e baixa por venda.
- Backend: testes de venda online com cliente opcional, consulta por cliente, rejeicao por estoque insuficiente e arredondamento monetario em item fracionado.
- Backend: testes de criacao, atualizacao, filtros e autorizacao basica de clientes.
- Backend: teste de `GET /api/system/info` validando envelope e roles.
- Backend: testes de fornecedores, sincronizacao offline, resolucao manual de conflitos offline e produtos mais vendidos.
- Admin web: testes de raiz/shell, `AuthService`, `AccessService`, `InventoryService`, `CustomerService`, `SalesService`, `CatalogService`, `SupplierService`, `OfflineService`, interceptor JWT, validacao/erro do login, timeout de requisicao sem resposta e formularios/paginas de usuarios, estoque, categorias, produtos, clientes, fornecedores e conflitos offline.
- PDV desktop: teste de `OfflineStorageConfig` validando caminho SQLite/JDBC URL, fila offline SQLite, catalogo local SQLite, corpo HTTP, login e parsing das consultas online.

## Estado de verificacao desta atualizacao

Durante a fatia de offline, relatorios e auditoria de cobertura:

- `backend-api`: `mvn test` passou com 33 testes.
- `admin-web`: `npm test -- --watch=false --progress=false` passou com 56 specs.
- `pdv-desktop`: `mvn test` passou com 8 testes.

Esta atualizacao de documentacao nao altera codigo de aplicacao, contratos, migrations ou testes.

## Diretrizes

- Para bug fix, adicionar teste de regressao quando pratico.
- Para nova fatia de backend, testar regra de dominio e contrato HTTP principal.
- Para nova tela Angular, testar renderizacao basica, integracao com service e estados de erro relevantes.
- Para PDV/offline, testar preservacao local da venda antes de testar sincronizacao.
- Nao remover ou enfraquecer teste para obter build verde sem entender a causa.
