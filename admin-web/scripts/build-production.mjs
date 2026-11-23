import { spawnSync } from 'node:child_process';

const apiBaseUrl = process.env.MERCADO_ONE_API_BASE_URL?.trim();
if (!apiBaseUrl || !/^https?:\/\/.+/i.test(apiBaseUrl)) {
  console.error('Defina MERCADO_ONE_API_BASE_URL com a URL publica da API, por exemplo https://mercado-one-api.azurewebsites.net');
  process.exit(1);
}

// Canais da tela "Fale com o suporte". São opcionais: o que ficar vazio não aparece na tela.
const supportWhatsapp = process.env.MERCADO_ONE_SUPPORT_WHATSAPP?.trim() ?? '';
const supportEmail = process.env.MERCADO_ONE_SUPPORT_EMAIL?.trim() ?? '';
const supportHours = process.env.MERCADO_ONE_SUPPORT_HOURS?.trim() ?? '';
if (supportWhatsapp && !/^55\d{10,11}$/.test(supportWhatsapp)) {
  console.error('MERCADO_ONE_SUPPORT_WHATSAPP deve ter so digitos, com 55 e DDD, por exemplo 5517999999999');
  process.exit(1);
}
if (supportEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(supportEmail)) {
  console.error('MERCADO_ONE_SUPPORT_EMAIL deve ser um e-mail valido');
  process.exit(1);
}

const result = spawnSync(
  process.execPath,
  [
    './node_modules/@angular/cli/bin/ng',
    'build',
    `--define=MERCADO_ONE_API_BASE_URL=${JSON.stringify(apiBaseUrl)}`,
    `--define=MERCADO_ONE_SUPPORT_WHATSAPP=${JSON.stringify(supportWhatsapp)}`,
    `--define=MERCADO_ONE_SUPPORT_EMAIL=${JSON.stringify(supportEmail)}`,
    `--define=MERCADO_ONE_SUPPORT_HOURS=${JSON.stringify(supportHours)}`,
  ],
  { stdio: 'inherit' },
);

process.exit(result.status ?? 1);
