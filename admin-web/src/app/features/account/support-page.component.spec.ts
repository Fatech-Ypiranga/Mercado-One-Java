import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { SupportPageComponent } from './support-page.component';
import { formatWhatsapp, SUPPORT_CONTACTS, SupportContacts, whatsappLink } from './support-contacts';

describe('SupportPageComponent', () => {
  function render(contacts: SupportContacts) {
    TestBed.configureTestingModule({
      imports: [SupportPageComponent],
      providers: [provideRouter([]), { provide: SUPPORT_CONTACTS, useValue: contacts }],
    });
    const fixture = TestBed.createComponent(SupportPageComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const channels = (root: HTMLElement) => Array.from(root.querySelectorAll('.login-channel'));

  it('shows only the configured channels', () => {
    const root = render({ whatsapp: '5517992072135', email: '', hours: '' });

    expect(channels(root).length).toBe(1);
    const whatsapp = channels(root)[0] as HTMLAnchorElement;
    expect(whatsapp.getAttribute('href')).toBe('https://wa.me/5517992072135');
    expect(whatsapp.textContent).toContain('(17) 99207-2135');
    expect(whatsapp.getAttribute('rel')).toContain('noopener');
    expect(root.querySelector('.login-hours')).toBeNull();
  });

  it('shows e-mail and opening hours when they are configured', () => {
    const root = render({ whatsapp: '5517992072135', email: 'suporte@mercado.test', hours: 'segunda a sexta, das 8h às 18h' });

    expect(channels(root).length).toBe(2);
    expect((channels(root)[1] as HTMLAnchorElement).getAttribute('href')).toBe('mailto:suporte@mercado.test');
    expect(root.querySelector('.login-hours')?.textContent).toContain('segunda a sexta, das 8h às 18h');
  });

  it('never invents a contact: with nothing configured it says so', () => {
    const root = render({ whatsapp: '', email: '', hours: '' });

    expect(channels(root).length).toBe(0);
    expect(root.textContent).toContain('ainda não foram configurados');
    expect(root.textContent).not.toContain('exemplo.com.br');
  });

  it('ignores a malformed WhatsApp number instead of building a broken link', () => {
    const root = render({ whatsapp: '12345', email: '', hours: '' });

    expect(channels(root).length).toBe(0);
    expect(root.querySelector('a[href^="https://wa.me"]')).toBeNull();
  });

  it('links back to the login', () => {
    const root = render({ whatsapp: '5517992072135', email: '', hours: '' });

    expect(Array.from(root.querySelectorAll('a')).map((a) => a.getAttribute('href'))).toContain('/login');
  });
});

describe('support contact helpers', () => {
  it('builds a WhatsApp link only for 55 + DDD + 8 or 9 digits', () => {
    expect(whatsappLink('5517992072135')).toBe('https://wa.me/5517992072135');
    expect(whatsappLink('551733334444')).toBe('https://wa.me/551733334444');
    expect(whatsappLink('17992072135')).toBeNull();
    expect(whatsappLink('')).toBeNull();
    expect(whatsappLink('55 17 99207-2135')).toBeNull();
  });

  it('formats Brazilian numbers and leaves unknown text as it came', () => {
    expect(formatWhatsapp('5517992072135')).toBe('(17) 99207-2135');
    expect(formatWhatsapp('551733334444')).toBe('(17) 3333-4444');
    expect(formatWhatsapp('abc')).toBe('abc');
  });
});
