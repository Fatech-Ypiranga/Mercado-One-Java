import { InjectionToken } from '@angular/core';

import { environment } from '../../../environments/environment';

/** Canais de suporte mostrados na tela "Fale com o suporte". Campo vazio = canal não configurado, não aparece. */
export interface SupportContacts {
  /** Só dígitos, com 55 e DDD. Exemplo: 5517999999999. */
  whatsapp: string;
  email: string;
  /** Texto livre, por exemplo "segunda a sexta, das 8h às 18h". */
  hours: string;
}

export const SUPPORT_CONTACTS = new InjectionToken<SupportContacts>('SUPPORT_CONTACTS', {
  providedIn: 'root',
  factory: () => ({
    whatsapp: environment.supportWhatsapp,
    email: environment.supportEmail,
    hours: environment.supportHours,
  }),
});

/** Link do WhatsApp, ou null se o número não for válido (55 + DDD + 8 ou 9 dígitos). */
export function whatsappLink(digits: string): string | null {
  return /^55\d{10,11}$/.test(digits.trim()) ? `https://wa.me/${digits.trim()}` : null;
}

/** 5517992072135 vira (17) 99207-2135. Devolve o texto original se não reconhecer o formato. */
export function formatWhatsapp(digits: string): string {
  const match = /^55(\d{2})(\d{4,5})(\d{4})$/.exec(digits.trim());
  return match ? `(${match[1]}) ${match[2]}-${match[3]}` : digits;
}
