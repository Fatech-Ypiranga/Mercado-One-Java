import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { OfflineService } from './offline.service';

describe('OfflineService', () => {
  let offline: OfflineService;
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    offline = TestBed.inject(OfflineService);
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

  it('lists pending offline conflicts', () => {
    offline.listConflicts('PENDING').subscribe();

    const request = http.expectOne('http://localhost:8080/api/offline/sales/conflicts?status=PENDING');
    expect(request.request.method).toBe('GET');
    request.flush({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  });

  it('can list offline conflicts without a status filter', () => {
    offline.listConflicts(null).subscribe();

    const request = http.expectOne('http://localhost:8080/api/offline/sales/conflicts');
    expect(request.request.method).toBe('GET');
    request.flush({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  });

  it('resolves an offline conflict', () => {
    offline.resolveConflict(10, { action: 'REJECT', note: 'Venda duplicada.' }).subscribe();

    const request = http.expectOne('http://localhost:8080/api/offline/sales/conflicts/10/resolve');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ action: 'REJECT', note: 'Venda duplicada.' });
    request.flush({ success: true, data: null, error: null, timestamp: new Date().toISOString() });
  });
});
