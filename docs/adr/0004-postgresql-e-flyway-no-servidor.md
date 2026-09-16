# PostgreSQL e Flyway no servidor

O backend usa PostgreSQL como banco central e Flyway como fonte de verdade para evolucao do schema. Essa decisao torna mudancas de dados revisaveis e evita depender de criacao automatica de tabelas pelo Hibernate em runtime.
