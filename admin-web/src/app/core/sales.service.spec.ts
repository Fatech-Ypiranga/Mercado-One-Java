import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { SalesService } from './sales.service';

describe('SalesService', () => {
  let sales: SalesService;
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    sales = TestBed.inject(SalesService);
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

  it('lists sales with report filters', () => {
    sales.listSales({
      from: '2026-09-01T00:00:00Z',
      to: '2026-09-30T23:59:59Z',
      operatorUserId: 2,
      customerId: 10,
      status: 'CONFIRMED',
      page: 1,
      size: 20,
    }).subscribe();

    const request = http.expectOne(
      'http://localhost:8080/api/sales?from=2026-09-01T00:00:00Z&to=2026-09-30T23:59:59Z&operatorUserId=2&customerId=10&status=CONFIRMED&page=1&size=20',
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBeTrue();
    request.flush({
      success: true,
      data: { items: [], page: { page: 1, size: 20, totalElements: 0, totalPages: 0 }, totals: { saleCount: 0, totalAmount: 0, totalItems: 0, totalsByPaymentMethod: {} } },
      error: null,
      timestamp: new Date().toISOString(),
    });
  });

  it('lists top products with report filters', () => {
    sales.topProducts({
      from: '2026-09-01T00:00:00Z',
      to: '2026-09-30T23:59:59Z',
      status: 'CONFIRMED',
    }, 5).subscribe();

    const request = http.expectOne(
      'http://localhost:8080/api/sales/top-products?from=2026-09-01T00:00:00Z&to=2026-09-30T23:59:59Z&status=CONFIRMED&limit=5',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  });
});
