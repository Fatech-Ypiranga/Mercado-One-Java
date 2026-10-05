# Backend em fatias verticais

O backend do Mercado One é organizado por capacidades de negócio dentro de `modules/`, em vez de camadas técnicas globais para todo o sistema. Essa decisão preserva propriedade de domínio por módulo e evita que regras de catálogo, estoque, vendas, clientes, acesso e offline se espalhem por pacotes compartilhados.
