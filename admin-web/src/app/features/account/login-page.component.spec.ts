import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject, throwError } from 'rxjs';

import { AuthService } from '../../core/auth.service';
import { LoginPageComponent } from './login-page.component';

describe('LoginPageComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();
  });

  const submitButton = (root: HTMLElement) => root.querySelector('.login-submit') as HTMLButtonElement;

  it('shows validation feedback when submitted empty', () => {
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.componentInstance['form'].setValue({ login: '', password: '' });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Informe o login.');
    expect(fixture.nativeElement.textContent).toContain('Informe a senha.');
  });

  it('shows a generic message for invalid credentials, without saying which field is wrong', () => {
    const auth = TestBed.inject(AuthService);
    spyOn(auth, 'login').and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.componentInstance['form'].setValue({ login: 'admin', password: 'wrong' });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(alert.textContent).toContain('Login ou senha incorretos. Confira os dados e tente de novo.');
    expect(fixture.nativeElement.querySelector('#login').getAttribute('aria-invalid')).toBe('true');
    expect(fixture.nativeElement.querySelector('#senha').getAttribute('aria-invalid')).toBe('true');
  });

  it('re-enables login and explains when the system is unreachable', () => {
    const auth = TestBed.inject(AuthService);
    spyOn(auth, 'login').and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.componentInstance['form'].setValue({ login: 'admin', password: 'admin123' });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const button = submitButton(fixture.nativeElement);
    expect(button.disabled).toBeFalse();
    expect(button.textContent).toContain('Entrar');
    expect(fixture.nativeElement.textContent).toContain('Não foi possível conectar ao sistema.');
  });

  it('submits only once while a login request is pending', () => {
    const auth = TestBed.inject(AuthService);
    const pending = new Subject<never>();
    spyOn(auth, 'login').and.returnValue(pending);
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.componentInstance['form'].setValue({ login: 'admin', password: 'admin123' });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const button = submitButton(fixture.nativeElement);
    expect(auth.login).toHaveBeenCalledTimes(1);
    expect(button.disabled).toBeTrue();
    expect(button.getAttribute('aria-busy')).toBe('true');
    expect(button.textContent).toContain('Entrando');
    pending.complete();
  });

  it('shows and hides the password with an accessible toggle', () => {
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const password = root.querySelector('#senha') as HTMLInputElement;
    const toggle = root.querySelector('.login-eye') as HTMLButtonElement;

    expect(password.type).toBe('password');
    expect(toggle.getAttribute('aria-label')).toBe('Mostrar senha');
    expect(toggle.getAttribute('aria-pressed')).toBe('false');

    toggle.click();
    fixture.detectChanges();

    expect(password.type).toBe('text');
    expect(toggle.getAttribute('aria-label')).toBe('Ocultar senha');
    expect(toggle.getAttribute('aria-pressed')).toBe('true');

    toggle.click();
    fixture.detectChanges();
    expect(password.type).toBe('password');
  });

  it('links to the forgot-password and support screens', () => {
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.detectChanges();
    const links = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a')).map((a) => a.getAttribute('href'));

    expect(links).toContain('/esqueci-senha');
    expect(links).toContain('/suporte');
  });

  it('puts the screen title in focus when it opens', () => {
    const fixture = TestBed.createComponent(LoginPageComponent);
    document.body.appendChild(fixture.nativeElement);
    fixture.detectChanges();

    expect(document.activeElement).toBe(fixture.nativeElement.querySelector('h1'));
    fixture.nativeElement.remove();
  });
});
