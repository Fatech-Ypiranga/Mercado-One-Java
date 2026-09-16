import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify({ ignoreCancelled: true });
  });

  it('stores a successful login user session without localStorage token', () => {
    auth.login({ login: 'admin', password: 'admin123' }).subscribe();

    const request = http.expectOne('http://localhost:8080/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBeTrue();
    request.flush({
      success: true,
      data: {
        accessToken: 'jwt-token',
        tokenType: 'Bearer',
        expiresAt: new Date(Date.now() + 60000).toISOString(),
        user: { id: 1, nome: 'Administrador', login: 'admin', perfil: 'ADMIN' },
      },
      error: null,
      timestamp: new Date().toISOString(),
    });

    expect(auth.session()?.user.login).toBe('admin');
    expect(localStorage.getItem('mercado-one-admin-session')).toBeNull();
  });

  it('uses credentials for protected requests', () => {
    auth.session.set({
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 1, nome: 'Administrador', login: 'admin', perfil: 'ADMIN' },
    });

    auth.me().subscribe();

    const request = http.expectOne('http://localhost:8080/api/auth/me');
    expect(request.request.withCredentials).toBeTrue();
    request.flush({
      success: true,
      data: { id: 1, nome: 'Administrador', login: 'admin', perfil: 'ADMIN', status: 'ACTIVE' },
      error: null,
      timestamp: new Date().toISOString(),
    });
  });

  it('checks the current user role against allowed roles', () => {
    auth.session.set({
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 1, nome: 'Gerente', login: 'gerente', perfil: 'GERENTE' },
    });

    expect(auth.hasAnyRole(['ADMIN', 'GERENTE'])).toBeTrue();
    expect(auth.hasAnyRole(['OPERADOR_CAIXA'])).toBeFalse();
  });

  it('times out login requests that never answer', fakeAsync(() => {
    let failed = false;

    auth.login({ login: 'admin', password: 'admin123' }).subscribe({
      error: () => {
        failed = true;
      },
    });

    http.expectOne('http://localhost:8080/api/auth/login');
    tick(10000);

    expect(failed).toBeTrue();
  }));
});
