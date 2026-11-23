// Gera os tokens da marca a partir de docs/marca/tokens.json (fonte única).
// Uso: node docs/marca/gerar-tokens.mjs
// Atualiza, em docs/marca/identidade-visual.html, o trecho entre os marcadores TOKENS
// (CSS usado pela página), CSS-SRC (bloco "Copiar CSS") e FX-SRC (bloco JavaFX).
import { readFileSync, writeFileSync } from 'node:fs';
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
  '  -fx-font-family: "Outfit";',
  '  -fx-base: -mo-bg;',
  '  -fx-background: -mo-bg;',
  '  -fx-text-base-color: -mo-ink;',
  '  -fx-accent: -mo-ocean;',
  '  -fx-focus-color: -mo-ocean;',
  '}',
  '.root.dark {',
  ...colors.filter((t) => val(t, 'dark') !== val(t, 'light')).map((t) => `  ${fxName(t.name)}: ${val(t, 'dark')};`),
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

const esc = (s) => s.replace(/</g, '&lt;');
let html = readFileSync(htmlPath, 'utf8');
const swap = (start, end, body) => {
  const re = new RegExp(`(${start})[\\s\\S]*?(${end})`);
  if (!re.test(html)) throw new Error(`Marcador não encontrado: ${start}`);
  html = html.replace(re, `$1\n${body}\n$2`);
};
swap('/\\* TOKENS:INICIO \\*/', '/\\* TOKENS:FIM \\*/', css);
swap('<!-- CSS-SRC:INICIO -->', '<!-- CSS-SRC:FIM -->', `<pre id="css">${esc(css)}</pre>`);
swap('<!-- FX-SRC:INICIO -->', '<!-- FX-SRC:FIM -->', `<pre id="fx">${esc(fx)}</pre>`);
writeFileSync(htmlPath, html);
console.log(`identidade-visual.html atualizado: ${colors.length} cores, ${dark.length} com valor próprio no modo escuro.`);
