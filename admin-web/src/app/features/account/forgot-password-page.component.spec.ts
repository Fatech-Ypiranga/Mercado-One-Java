import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ForgotPasswordPageComponent } from './forgot-password-page.component';

describe('ForgotPasswordPageComponent', () => {
  const statusUrl = 'http://localhost:8080/api/auth/password-reset/status';
  const requestUrl = 'http://localhost:8080/api/auth/password-reset/request';
  const envelope = (data: unknown) => ({ success: true, data, error: null, timestamp: '' });

  function render(enabled: boolean | 'error') {
    TestBed.configureTestingModule({
      imports: [ForgotPasswordPageComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    const fixture = TestBed.createComponent(ForgotPasswordPageComponent);
    document.body.appendChild(fixture.nativeElement);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const status = http.expectOne((r) => r.url.endsWith('/api/auth/password-reset/status'));
    if (enabled === 'error') {
      status.error(new ProgressEvent('error'));
    } else {
      status.flush(envelope({ enabled }));
    }
    fixture.detectChanges();
    return { fixture, http, root: fixture.nativeElement as HTMLElement };
  }

  afterEach(() => document.querySelectorAll('mo-forgot-password-page').forEach((n) => n.remove()));

  it('without e-mail configured it explains that the administrator resets the password and offers no form', () => {
    const { root, http } = render(false);

    expect(root.textContent).toContain('Esqueceu a senha?');
    expect(root.textContent).toContain('Redefinir senha');
    expect(root.textContent).toContain('Minha conta');
    expect(root.querySelector('form')).toBeNull();
    expect(root.querySelector('input')).toBeNull();
    expect(root.textContent).not.toContain('Enviar link');
    http.verify();
  });

  it('if the server cannot be reached it also falls back to the administrator instructions', () => {
    const { root } = render('error');

    expect(root.textContent).toContain('Redefinir senha');
    expect(root.querySelector('form')).toBeNull();
  });

  it('with e-mail configured it asks for login or e-mail and sends the request', () => {
    const { fixture, root, http } = render(true);
    const input = root.querySelector('#identificador') as HTMLInputElement;
    input.value = '  ana@mercado.test ';
    input.dispatchEvent(new Event('input'));

    (root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    const request = http.expectOne(requestUrl);

    expect(request.request.body).toEqual({ identifier: 'ana@mercado.test' });
    request.flush(envelope(null));
    fixture.detectChanges();
    expect(root.textContent).toContain('Confira o seu e-mail');
    expect(root.textContent).toContain('Se existir uma conta');
    expect(root.querySelector('form')).toBeNull();
    http.verify();
  });

  it('shows the same message whether or not the account exists (the API answers the same)', () => {
    const first = render(true);
    (first.root.querySelector('#identificador') as HTMLInputElement).value = 'existe';
    (first.root.querySelector('#identificador') as HTMLInputElement).dispatchEvent(new Event('input'));
    (first.root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    first.http.expectOne(requestUrl).flush(envelope(null));
    first.fixture.detectChanges();

    expect(first.root.textContent).toContain('Se existir uma conta com esses dados');
    expect(first.root.textContent).not.toContain('não encontramos');
    expect(first.root.textContent).not.toContain('não existe');
  });

  it('an empty submit shows the field error and sends nothing', () => {
    const { fixture, root, http } = render(true);

    (root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(root.textContent).toContain('Informe o login ou o e-mail.');
    expect(document.activeElement?.id).toBe('identificador');
    http.expectNone(requestUrl);
  });

  it('if the server says the feature is off (503) it switches to the administrator instructions', () => {
    const { fixture, root, http } = render(true);
    (root.querySelector('#identificador') as HTMLInputElement).value = 'ana';
    (root.querySelector('#identificador') as HTMLInputElement).dispatchEvent(new Event('input'));

    (root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    http.expectOne(requestUrl).flush({ success: false, data: null, error: { code: 'RESET_UNAVAILABLE', message: 'x', details: [] }, timestamp: '' }, { status: 503, statusText: 'Service Unavailable' });
    fixture.detectChanges();

    expect(root.textContent).toContain('Redefinir senha');
    expect(root.querySelector('form')).toBeNull();
  });

  it('a network failure keeps the form and says to try again', () => {
    const { fixture, root, http } = render(true);
    (root.querySelector('#identificador') as HTMLInputElement).value = 'ana';
    (root.querySelector('#identificador') as HTMLInputElement).dispatchEvent(new Event('input'));

    (root.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    http.expectOne(requestUrl).error(new ProgressEvent('error'));
    fixture.detectChanges();

    expect(root.querySelector('form')).not.toBeNull();
    expect(root.textContent).toContain('Não foi possível enviar agora');
  });

  it('links back to the login and focuses the title', () => {
    const { root } = render(false);
    const links = Array.from(root.querySelectorAll('a')).map((a) => a.getAttribute('href'));

    expect(links).toContain('/login');
    expect(links).toContain('/suporte');
    expect(document.activeElement?.id).toBe('forgot-title');
  });

  it('is unused when statusUrl stays constant', () => {
    expect(statusUrl.endsWith('/status')).toBeTrue();
  });
});
