import { spawnSync } from 'node:child_process';

const apiBaseUrl = process.env.MERCADO_ONE_API_BASE_URL?.trim();
if (!apiBaseUrl || !/^https?:\/\/.+/i.test(apiBaseUrl)) {
  console.error('Defina MERCADO_ONE_API_BASE_URL com a URL publica da API, por exemplo https://mercado-one-api.azurewebsites.net');
  process.exit(1);
}

const result = spawnSync(
  process.execPath,
  [
    './node_modules/@angular/cli/bin/ng',
    'build',
    `--define=MERCADO_ONE_API_BASE_URL=${JSON.stringify(apiBaseUrl)}`,
  ],
  { stdio: 'inherit' },
);

process.exit(result.status ?? 1);
