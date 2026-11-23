declare const MERCADO_ONE_API_BASE_URL: string;
declare const MERCADO_ONE_SUPPORT_WHATSAPP: string;
declare const MERCADO_ONE_SUPPORT_EMAIL: string;
declare const MERCADO_ONE_SUPPORT_HOURS: string;

export const environment = {
  production: true,
  apiBaseUrl: MERCADO_ONE_API_BASE_URL,
  // Canais de suporte vêm do build (opcionais): veja scripts/build-production.mjs.
  supportWhatsapp: MERCADO_ONE_SUPPORT_WHATSAPP,
  supportEmail: MERCADO_ONE_SUPPORT_EMAIL,
  supportHours: MERCADO_ONE_SUPPORT_HOURS,
};
