# PostgreSQL e Flyway no servidor

O backend usa PostgreSQL como banco central e Flyway como fonte de verdade para evolução do schema. Essa decisão torna mudanças de dados revisáveis e evita depender de criação automática de tabelas pelo Hibernate em runtime.
