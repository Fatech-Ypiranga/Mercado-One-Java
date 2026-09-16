# Backend em fatias verticais

O backend do Mercado One e organizado por capacidades de negocio dentro de `modules/`, em vez de camadas tecnicas globais para todo o sistema. Essa decisao preserva propriedade de dominio por modulo e evita que regras de catalogo, estoque, vendas, clientes, acesso e offline se espalhem por pacotes compartilhados.
