# Backend API

API REST do Mercado One: contratos de servidor, regras centrais do MVP e persistencia em PostgreSQL.

O que esta implementado: [Estado atual](../docs/estado-atual.md). Fatias e acoplamento: [Arquitetura](../docs/arquitetura.md).

## Comandos

```bash
mvn spring-boot:run
mvn test
```

`mvn spring-boot:run` le o `.env` da raiz do repositorio (ou do diretorio atual). A variavel obrigatoria e `MERCADO_ONE_JWT_SECRET`. Os testes usam H2 em memoria, com Flyway desligado e `ddl-auto=create-drop`; o detalhe esta em [Dados e migracoes](../docs/dados-e-migracoes.md).

## Documentacao

- [Contratos de API](../docs/api-contratos.md)
- [Dados e migracoes](../docs/dados-e-migracoes.md)
- [Seguranca](../docs/seguranca.md)
