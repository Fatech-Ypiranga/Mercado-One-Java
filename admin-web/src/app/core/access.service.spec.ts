import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AccessService } from './access.service';
import { AuthService } from './auth.service';

describe('AccessService', () => {
  let access: AccessService;
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    access = TestBed.inject(AccessService);
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    auth.session.set({
      expiresAt: new Date(Date.now() + 60000).toISOString(),
      user: { id: 1, nome: 'Administrador', login: 'admin', perfil: 'ADMIN' },
    });
  });

  afterEach(() => {
    http.verify();
  });

  it('lists users with filters and credentials', () => {
    access.listUsers({ search: 'ger', role: 'GERENTE', active: true }).subscribe();

    const request = http.expectOne('http://localhost:8080/api/access/users?search=ger&role=GERENTE&active=true');
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBeTrue();
    request.flush({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  });

  it('creates a user without exposing password in response contract', () => {
    access.createUser({
      nome: 'Gerente Loja',
      login: 'gerente',
      password: 'senha123',
      perfil: 'GERENTE',
      active: true,
    }).subscribe((response) => {
      expect('passwordHash' in (response.data ?? {})).toBeFalse();
    });

    const request = http.expectOne('http://localhost:8080/api/access/users');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.password).toBe('senha123');
    request.flush({
      success: true,
      data: {
        id: 2,
        nome: 'Gerente Loja',
        login: 'gerente',
        perfil: 'GERENTE',
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      },
      error: null,
      timestamp: new Date().toISOString(),
    });
  });
});
