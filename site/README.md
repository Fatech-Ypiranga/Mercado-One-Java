# Site

Página institucional do mercadoOne: HTML estático, sem build. Abra `index.html` no navegador ou sirva a pasta (`python -m http.server` dentro de `site/`).

## Conteúdo

O texto só afirma o que o código já faz, conforme o [Backlog MVP](../docs/produto/backlog-mvp.md), e lista o que ainda não existe (nota fiscal, TEF, validade e lote, multi-loja, app mobile). Ao entregar uma funcionalidade nova, atualize a seção "O que o sistema faz hoje". Não há números de clientes, avaliações nem depoimentos: só entram quando forem reais e autorizados por escrito.

## Visual

Segue a [identidade visual](../docs/marca/identidade-visual.html). `brand-tokens.css` é gerado de `docs/marca/tokens.json` por `node docs/marca/gerar-tokens.mjs` (na raiz) e não deve ser editado à mão. A fonte Outfit fica em `fonts/` (licença OFL em `OFL-Outfit.txt`), sem carregar nada de terceiros. O modo claro/escuro segue o sistema operacional na primeira visita e guarda a escolha em `localStorage` (`mo-theme`). As partículas ficam no fundo da página.

## Pendências antes de publicar

- **WhatsApp:** a constante `WHATSAPP` no fim de `index.html` está com um número de teste (só dígitos, com 55 e DDD). Troque pelo número oficial da empresa antes de publicar: ele fica visível a qualquer visitante. Se ficar vazia, o formulário mostra a mensagem pronta em vez de abrir o WhatsApp.
- **Privacidade e termos:** `privacidade.html` e `termos.html` são rascunhos baseados no que o site coleta (nome, mercado, caixas e preferência de contato, enviados só pelo WhatsApp; sem cookies nem rastreadores). Preencha os campos em amarelo (razão social, CNPJ, encarregado, prazo de guarda, preço, suporte, foro) e faça revisão jurídica antes de publicar. Se o site passar a usar análise de acesso ou outro serviço de terceiros, atualize a política.
- **Imagens:** `img/` tem capturas do admin com dados de exemplo. Refaça com o sistema real quando houver piloto.
- **Logo:** `brand/` tem os SVGs vetorizados automaticamente; peça a um designer para refinar.
