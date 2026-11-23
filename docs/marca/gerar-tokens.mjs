// Gera os tokens da marca a partir de docs/marca/tokens.json (fonte única).
// Uso: node docs/marca/gerar-tokens.mjs
// Atualiza, em docs/marca/identidade-visual.html, o trecho entre os marcadores TOKENS
// (CSS usado pela página), CSS-SRC (bloco "Copiar CSS") e FX-SRC (bloco JavaFX).
// Gera também admin-web/src/brand-tokens.css, site/brand-tokens.css e os brand-tokens*.css do PDV,
// e copia os logos de docs/marca/logo para o admin, o site e o PDV.
import { copyFileSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const tokens = JSON.parse(readFileSync(join(here, 'tokens.json'), 'utf8'));
const htmlPath = join(here, 'identidade-visual.html');

const colors = tokens.color.tokens;
const scales = ['spacing', 'radius', 'shadow'].flatMap((f) => tokens[f]?.tokens ?? []);
const val = (t, theme) => (typeof t.value === 'string' ? t.value : t.value[theme] ?? t.value.light);

const light = colors.map((t) => `  --${t.name}: ${val(t, 'light')};`);
const dark = colors.filter((t) => val(t, 'dark') !== val(t, 'light')).map((t) => `  --${t.name}: ${val(t, 'dark')};`);
const rest = [
  ...scales.map((t) => `  --${t.name}: ${t.value};`),
  `  --font-sans: ${tokens.type.families.sans};`,
  `  --font-mono: ${tokens.type.families.mono};`,
];

const css = [
  `/* Gerado de docs/marca/tokens.json (v${tokens.version}). Não edite à mão. */`,
  ':root {', '  color-scheme: light;', ...light, ...rest, '}',
  '[data-theme="dark"] {', '  color-scheme: dark;', ...dark, '}',
].join('\n');

const fxName = (n) => `-mo-${n}`;
const fx = [
  `/* Gerado de docs/marca/tokens.json (v${tokens.version}). Não edite à mão. */`,
  '.root {',
  ...colors.map((t) => `  ${fxName(t.name)}: ${val(t, 'light')};`),
  '  /* Fonte: a Outfit só funciona se o app carregar o arquivo TTF (Font.loadFont); o CSS do app define a família. */',
  '  -fx-base: -mo-bg;',
  '  -fx-background: -mo-bg;',
  '  -fx-text-base-color: -mo-ink;',
  '  -fx-accent: -mo-ocean;',
  '  -fx-focus-color: -mo-ocean;',
  '}',
  '.button.primary { -fx-background-color: -mo-action; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-min-height: 44; }',
  '.button.primary:hover { -fx-background-color: -mo-action-hover; }',
  '.button.secondary { -fx-background-color: transparent; -fx-border-color: -mo-line; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-text-fill: -mo-ink; -fx-min-height: 44; }',
  '.card { -fx-background-color: -mo-surface; -fx-border-color: -mo-line; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 24; }',
  '.sidebar { -fx-background-color: -mo-sidebar; -fx-border-color: transparent -mo-line transparent transparent; }',
  '.sidebar .selected { -fx-background-color: -mo-selected; -fx-border-color: transparent transparent transparent -mo-ocean; -fx-border-width: 0 0 0 3; }',
  '.pill-ok { -fx-background-color: -mo-success-bg; -fx-text-fill: -mo-success-ink; -fx-background-radius: 999; -fx-padding: 3 10; -fx-font-size: 13; }',
  '.pill-warn { -fx-background-color: -mo-warning-bg; -fx-text-fill: -mo-warning-ink; -fx-background-radius: 999; -fx-padding: 3 10; -fx-font-size: 13; }',
  '.pill-danger { -fx-background-color: -mo-danger-bg; -fx-text-fill: -mo-danger-ink; -fx-background-radius: 999; -fx-padding: 3 10; -fx-font-size: 13; }',
  '.chart-bar { -fx-bar-fill: -mo-chart-bar; }',
  '.chart-peak { -fx-bar-fill: -mo-chart-peak; }',
].join('\n');

// Modo escuro do JavaFX: um arquivo à parte; o PDV aplica estas variáveis no nó raiz com setStyle.
// (O seletor composto ".root.dark" não sobrepõe as variáveis de forma confiável no JavaFX.)
const fxDark = [
  `/* Gerado de docs/marca/tokens.json (v${tokens.version}). Não edite à mão. Modo escuro: aplique estas variáveis no nó raiz (setStyle). */`,
  '.root {',
  ...colors.filter((t) => val(t, 'dark') !== val(t, 'light')).map((t) => `  ${fxName(t.name)}: ${val(t, 'dark')};`),
  '}',
].join('\n');

const esc = (s) => s.replace(/</g, '&lt;');
let html = readFileSync(htmlPath, 'utf8');
const swap = (start, end, body) => {
  const re = new RegExp(`(${start})[\\s\\S]*?(${end})`);
  if (!re.test(html)) throw new Error(`Marcador não encontrado: ${start}`);
  html = html.replace(re, `$1\n${body}\n$2`);
};
swap('/\\* TOKENS:INICIO \\*/', '/\\* TOKENS:FIM \\*/', css);
swap('<!-- CSS-SRC:INICIO -->', '<!-- CSS-SRC:FIM -->', `<pre id="css">${esc(css)}</pre>`);
swap('<!-- FX-SRC:INICIO -->', '<!-- FX-SRC:FIM -->', `<pre id="fx">${esc(fx + '\n\n/* ── brand-tokens-dark.css ── */\n' + fxDark)}</pre>`);
writeFileSync(htmlPath, html);

// Admin web (Angular): mesmo CSS, importado pelo src/styles.css.
const adminPath = join(here, '..', '..', 'admin-web', 'src', 'brand-tokens.css');
writeFileSync(adminPath, css + '\n');
console.log('admin-web/src/brand-tokens.css atualizado.');

// Site institucional (HTML estático em site/).
writeFileSync(join(here, '..', '..', 'site', 'brand-tokens.css'), css + '\n');
console.log('site/brand-tokens.css atualizado.');

// PDV (JavaFX): variáveis -mo-* e classes base, carregadas antes de styles/pdv.css.
const pdvPath = join(here, '..', '..', 'pdv-desktop', 'src', 'main', 'resources', 'styles', 'brand-tokens.css');
writeFileSync(pdvPath, fx + '\n');
writeFileSync(pdvPath.replace('brand-tokens.css', 'brand-tokens-dark.css'), fxDark + '\n');
console.log('pdv-desktop/src/main/resources/styles/brand-tokens.css e brand-tokens-dark.css atualizados.');
// Logos: a fonte única é docs/marca/logo; cada app recebe só as cópias de que precisa.
const root = join(here, '..', '..');
const logoCopies = {
  'admin-web/public/brand': ['svg/mercadoone-simbolo.svg', 'svg/mercadoone-simbolo-negativa.svg', 'svg/favicon.svg'],
  'site/brand': ['svg/mercadoone-simbolo.svg', 'svg/mercadoone-simbolo-negativa.svg', 'svg/mercadoone-horizontal.svg', 'svg/mercadoone-horizontal-negativa.svg', 'svg/favicon.svg', 'png/favicon-32.png', 'png/apple-touch-icon.png'],
  'pdv-desktop/src/main/resources/brand': ['png/mercadoone-simbolo-192.png', 'png/mercadoone-simbolo-negativa-192.png', 'png/icone-512.png'],
};
for (const [dest, files] of Object.entries(logoCopies)) {
  mkdirSync(join(root, dest), { recursive: true });
  for (const f of files) copyFileSync(join(here, 'logo', f), join(root, dest, f.split('/').pop()));
  console.log(`${dest}: ${files.length} logos copiados de docs/marca/logo.`);
}
console.log(`identidade-visual.html atualizado: ${colors.length} cores, ${dark.length} com valor próprio no modo escuro.`);
