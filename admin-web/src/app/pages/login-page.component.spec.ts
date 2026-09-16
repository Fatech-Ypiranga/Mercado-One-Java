import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { throwError } from 'rxjs';

import { AuthService } from '../core/auth.service';
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

  it('shows validation feedback when submitted empty', () => {
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.componentInstance['form'].setValue({ login: '', password: '' });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Informe o login.');
    expect(fixture.nativeElement.textContent).toContain('Informe a senha.');
  });

  it('shows a friendly message for invalid credentials', () => {
    const auth = TestBed.inject(AuthService);
    spyOn(auth, 'login').and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.componentInstance['form'].setValue({ login: 'admin', password: 'wrong' });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Login ou senha invalidos.');
  });

  it('re-enables login and explains when the API is unavailable', () => {
    const auth = TestBed.inject(AuthService);
    spyOn(auth, 'login').and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.componentInstance['form'].setValue({ login: 'admin', password: 'admin123' });
    fixture.detectChanges();

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
    expect(button.disabled).toBeFalse();
    expect(button.textContent).toContain('Entrar');
    expect(fixture.nativeElement.textContent).toContain('Nao foi possivel conectar a API.');
  });
});
