import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';

import { ResetPasswordPageComponent } from './reset-password-page.component';

describe('ResetPasswordPageComponent', () => {
  const envelope = (data: unknown) => ({ success: true, data, error: null, timestamp: '' });
  const failure = (code: string, message: string) => ({ success: false, data: null, error: { code, message, details: [] }, timestamp: '' });

  function render(token: string | null, linkValid: boolean | 'error' = true) {
    TestBed.configureTestingModule({
      imports: [ResetPasswordPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(token === null ? {} : { token }) } } },
      ],
    });
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(ResetPasswordPageComponent);
    document.body.appendChild(fixture.nativeElement);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    if (token) {
      const check = http.expectOne((r) => r.url.endsWith('/api/auth/password-reset/check'));
      expect(check.request.body).toEqual({ token });
      if (linkValid === 'error') {
        check.error(new ProgressEvent('error'));
      } else {
        check.flush(envelope({ valid: linkValid }));
      }
      fixture.detectChanges();
    }
    return { fixture, http, navigate, root: fixture.nativeElement as HTMLElement };
  }

  function fill(root: HTMLElement, password: string, confirm: string) {
    const set = (id: string, value: string) => {
      const input = root.querySelector('#' + id) as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    };
    set('nova-senha', password);
    set('confirmar-senha', confirm);
  }

  const submit = (root: HTMLElement) => (root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

  afterEach(() => document.querySelectorAll('mo-reset-password-page').forEach((n) => n.remove()));

  it('a valid link opens the new password form', () => {
    const { root } = render('abc123');

    expect(root.textContent).toContain('Crie uma senha nova');
    expect(root.querySelector('#nova-senha')).not.toBeNull();
    expect(root.querySelector('#confirmar-senha')).not.toBeNull();
  });

  it('removes the token from the address bar as soon as it is read', () => {
    const { navigate } = render('abc123');

    expect(navigate).toHaveBeenCalledWith([], jasmine.objectContaining({ queryParams: {}, replaceUrl: true }));
  });

  it('a link with no token, or one the server does not accept, shows the expired screen', () => {
    const empty = render(null);
    expect(empty.root.textContent).toContain('Este link não vale mais');
    expect(empty.root.querySelector('a[href="/esqueci-senha"]')).not.toBeNull();
    expect(empty.root.querySelector('form')).toBeNull();
    empty.fixture.destroy();
    TestBed.resetTestingModule();

    const refused = render('vencido', false);
    expect(refused.root.textContent).toContain('Este link não vale mais');
    expect(refused.root.querySelector('a[href="/esqueci-senha"]')).not.toBeNull();
  });

  it('if the server cannot be reached it does not claim the link expired for certain', () => {
    const { root } = render('abc123', 'error');

    expect(root.textContent).toContain('Não foi possível conferir o link agora');
  });

  it('saves the new password and then offers to log in', () => {
    const { fixture, root, http } = render('abc123');
    fill(root, 'senha-nova-9', 'senha-nova-9');

    submit(root);
    const confirm = http.expectOne((r) => r.url.endsWith('/api/auth/password-reset/confirm'));
    expect(confirm.request.body).toEqual({ token: 'abc123', password: 'senha-nova-9' });
    confirm.flush(envelope(null));
    fixture.detectChanges();

    expect(root.textContent).toContain('Senha alterada');
    expect(root.querySelector('a[href="/login"]')).not.toBeNull();
    expect(root.querySelector('form')).toBeNull();
    http.verify();
  });

  it('refuses a short password and mismatching passwords without calling the API', () => {
    const { fixture, root, http } = render('abc123');

    fill(root, '123', '123');
    submit(root);
    fixture.detectChanges();
    expect(root.textContent).toContain('de 6 a 120 caracteres');

    fill(root, 'senha-nova-9', 'senha-nova-8');
    submit(root);
    fixture.detectChanges();
    expect(root.textContent).toContain('As duas senhas precisam ser iguais.');
    http.expectNone((r) => r.url.endsWith('/api/auth/password-reset/confirm'));
  });

  it('if the link was used or expired meanwhile, it switches to the expired screen', () => {
    const { fixture, root, http } = render('abc123');
    fill(root, 'senha-nova-9', 'senha-nova-9');

    submit(root);
    http.expectOne((r) => r.url.endsWith('/api/auth/password-reset/confirm'))
      .flush(failure('RESET_LINK_INVALID', 'Link invalido ou expirado.'), { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(root.textContent).toContain('Este link não vale mais');
  });

  it('keeps the form and shows the server message for other failures', () => {
    const { fixture, root, http } = render('abc123');
    fill(root, 'senha-nova-9', 'senha-nova-9');

    submit(root);
    http.expectOne((r) => r.url.endsWith('/api/auth/password-reset/confirm'))
      .flush(failure('BUSINESS_RULE', 'Senha deve ter entre 6 e 120 caracteres.'), { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(root.querySelector('form')).not.toBeNull();
    expect(root.textContent).toContain('Senha deve ter entre 6 e 120 caracteres.');
  });

  it('shows and hides both passwords with one button', () => {
    const { fixture, root } = render('abc123');
    const toggle = root.querySelector('.login-eye') as HTMLButtonElement;

    expect((root.querySelector('#nova-senha') as HTMLInputElement).type).toBe('password');
    toggle.click();
    fixture.detectChanges();

    expect((root.querySelector('#nova-senha') as HTMLInputElement).type).toBe('text');
    expect((root.querySelector('#confirmar-senha') as HTMLInputElement).type).toBe('text');
    expect(toggle.getAttribute('aria-pressed')).toBe('true');
  });
});
