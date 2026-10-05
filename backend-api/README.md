# Backend API

API REST do Mercado One: contratos de servidor, regras centrais do MVP e persistência em PostgreSQL.

O que está implementado: [Estado atual](../docs/estado-atual.md). Fatias e acoplamento: [Arquitetura](../docs/arquitetura.md).

## Comandos

```bash
mvn spring-boot:run
mvn test
```

`mvn spring-boot:run` lê o `.env` da raiz do repositório (ou do diretório atual). A variável obrigatória é `MERCADO_ONE_JWT_SECRET`. Os testes usam H2 em memória, com Flyway desligado e `ddl-auto=create-drop`; o detalhe está em [Dados e migrações](../docs/dados-e-migracoes.md).

## Documentação

- [Contratos de API](../docs/api-contratos.md)
- [Dados e migrações](../docs/dados-e-migracoes.md)
- [Segurança](../docs/seguranca.md)
