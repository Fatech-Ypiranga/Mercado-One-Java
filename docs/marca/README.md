# Marca

Identidade visual do mercadoOne. Para ver, abra [identidade-visual.html](identidade-visual.html) no navegador.

## Estrutura

```text
docs/marca/
  identidade-visual.html   Manual: logo, cores, tipografia, componentes, regras e tokens para copiar
  tokens.json              Fonte única de cores e medidas (claro e escuro)
  gerar-tokens.mjs         Gera os arquivos dos apps a partir do tokens.json e copia os logos
  logo/
    svg/                   Logos vetoriais (transparentes): horizontal e símbolo, em cor, negativa e uma cor; favicon
    png/                   Favicon, ícone do app (512) e símbolo em PNG para o PDV, que não lê SVG
    original/              JPGs entregues, com fundo branco (fonte da vetorização, não use nas telas)
```

## Mudar uma cor ou um logo

1. Cor ou medida: edite `tokens.json`. Logo: troque o arquivo em `logo/svg` ou `logo/png`.
2. Na raiz do repositório, rode `node docs/marca/gerar-tokens.mjs`.
3. Confira o `git diff` e os apps.

O gerador atualiza:

| Destino | O que recebe |
| --- | --- |
| `docs/marca/identidade-visual.html` | Tokens da página e os blocos "Copiar CSS" e JavaFX |
| `admin-web/src/brand-tokens.css` | Variáveis CSS (claro e `data-theme="dark"`) |
| `admin-web/public/brand/` | Símbolo (cor e negativa) e favicon |
| `site/brand-tokens.css`, `site/brand/` | Variáveis CSS, logos e ícones |
| `pdv-desktop/src/main/resources/styles/brand-tokens*.css` | Variáveis `-mo-*` do JavaFX (claro e escuro) |
| `pdv-desktop/src/main/resources/brand/` | Símbolo em PNG (cor e negativa) e ícone da janela |

Não edite esses destinos à mão: a próxima execução do gerador sobrescreve.

## Qual logo usar

| Onde | Arquivo |
| --- | --- |
| Fundo claro | `mercadoone-horizontal.svg` ou `mercadoone-simbolo.svg` |
| Fundo escuro e modo escuro | versões `-negativa` |
| Comprovante, impressora térmica, carimbo | versões `-uma-cor` (usam `currentColor`; preto por padrão) |
| Favicon e ícone do app | `favicon.svg` (quadrado, com margem) e os PNGs |

Regras de uso, área de proteção e o que evitar estão no manual.

## Como os SVGs foram feitos

Os SVGs foram vetorizados automaticamente a partir de `logo/original/mercadoone-horizontal.jpg`, em camadas (M, barras, "mercado" e "One"), com degradês refeitos à mão. O formato e a espessura do traço ficaram fiéis ao original; os sombreados do símbolo, como a base mais escura do M, são aproximados. **Um designer deve refinar o desenho** antes de uso em impressão ou fachada.

## Fonte

Outfit, sob a licença SIL Open Font License. Os arquivos ficam em `site/fonts/` e `pdv-desktop/src/main/resources/fonts/`; o admin usa o pacote `@fontsource/outfit`.

## Decisões que diferem do documento "versão final"

O documento de identidade colado em 23/11/2026 e o repositório divergiam em alguns pontos. Vale o que está em `tokens.json`:

| Ponto | Documento | Repositório | Por quê |
| --- | --- | --- | --- |
| Verde de ação | oliva #4A7A00 | esmeralda #2E7D5B | Fica na mesma família fria do azul (matiz perto de 157°). O oliva puxa para o amarelo e quebra a paleta. Contraste com texto branco é de 5:1 nos dois. |
| Menu lateral no claro | #122C4F | branco, separado por linha | Tela densa usada o dia todo: menu escuro e pesado ao lado de tabelas rouba a hierarquia do conteúdo. |
| Hover do botão no escuro | #3A8F69 | #276B4E | Texto branco sobre #3A8F69 dá cerca de 3,9:1, abaixo de 4,5:1. O hover escurece nos dois modos. |
| Borda de campo | 3:1 | token `field-border` #6F849B | 3,85:1 no claro e 3,97:1 no escuro (WCAG 1.4.11). |
| Login com campos vazios | alerta único | mensagem por campo e foco no primeiro inválido | A mensagem junto do campo diz onde está o erro (WCAG 3.3.1). |
| Esqueci a senha | link por e-mail | tela que explica a redefinição pelo administrador | O backend não envia e-mail. Não há promessa que o sistema não cumpra. |
| Altura dos controles | 48px | 44px no admin; 48px no login, site e PDV | 44px já atende o alvo de toque recomendado em telas densas. |

