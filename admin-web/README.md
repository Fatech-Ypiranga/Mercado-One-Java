# Admin Web

Administrativo web do Mercado One em Angular 22. Este subprojeto concentra a interface administrativa usada por administrador, gerente e perfis operacionais autorizados.

## Estado atual

Implementado:

- Aplicacao Angular standalone.
- Login com sessao por cookie HttpOnly.
- Shell lateral protegido com navegacao principal filtrada por perfil.
- Guards por perfil para rotas administrativas conforme autorizacao da API.
- Rotas para inicio, usuarios, produtos, categorias, estoque, vendas, conflitos offline, clientes e fornecedores.
- Dashboard inicial com briefing do dia, conflitos pendentes, totais de venda e indice por departamento.
- Telas funcionais de usuarios, categorias, produtos, estoque, vendas, conflitos offline, clientes e fornecedores com filtros, periodo de movimentacoes, formularios e estados de loading, vazio, erro e sucesso.
- Identidade visual de balcao: papel de talao, Petrona e IBM Plex, navegacao por departamento, tabelas como superficie principal e fichas contextuais.
- `ApiClientService`, `AuthService`, `AccessService`, `CatalogService`, `InventoryService`, `SalesService`, `CustomerService`, `SupplierService` e `OfflineService` tipados para os contratos iniciais da API.
- Ambiente local apontando para `http://localhost:8080`.
- Teste minimo do shell administrativo.

Credencial inicial de desenvolvimento: `admin` / `admin123`.

Ainda planejado:

- Tratamento padronizado completo de erros de API em todas as telas.

## Rotas atuais

```text
/login       Login do admin web
/            Dashboard inicial protegido
/usuarios    Gestao de usuarios e perfis
/produtos    Cadastro e consulta de produtos
/categorias  Cadastro e consulta de categorias
/estoque     Saldos, entradas, ajustes e movimentacoes
/vendas      Relatorio de vendas e historico por cliente
/offline     Resolucao administrativa de conflitos offline
/clientes    Cadastro, consulta e status de clientes
/fornecedores Cadastro, consulta e status de fornecedores
```

## Comandos

```bash
npm install
npm start
npm test -- --watch=false
npm run build
```

## Documentacao relacionada

- [Arquitetura](../docs/arquitetura.md)
- [Backlog MVP](../docs/backlog-mvp.md)
- [Contratos de API](../docs/api-contratos.md)
- [Testes e verificacao](../docs/testes-e-verificacao.md)
