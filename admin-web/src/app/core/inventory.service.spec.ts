import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { InventoryService } from './inventory.service';

describe('InventoryService', () => {
  let inventory: InventoryService;
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    inventory = TestBed.inject(InventoryService);
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

  it('lists balances with filters and bearer token', () => {
    inventory.listBalances({ search: 'caf', active: true }).subscribe();

    const request = http.expectOne('http://localhost:8080/api/inventory/balances?search=caf&active=true');
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBeTrue();
    request.flush({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  });

  it('registers an inventory entry', () => {
    inventory.registerEntry({
      productId: 10,
      quantity: 4.5,
      supplierId: 20,
      supplierName: 'Fornecedor',
      documentNumber: 'NF-1',
      note: null,
    }).subscribe();

    const request = http.expectOne('http://localhost:8080/api/inventory/entries');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(jasmine.objectContaining({ productId: 10, quantity: 4.5, supplierId: 20 }));
    request.flush({ success: true, data: null, error: null, timestamp: new Date().toISOString() });
  });

  it('lists movements with period filters', () => {
    inventory.listMovements({
      type: 'SALE',
      supplierId: 20,
      from: '2026-09-01T00:00:00Z',
      to: '2026-09-30T23:59:59Z',
    }).subscribe();

    const request = http.expectOne(
      'http://localhost:8080/api/inventory/movements?type=SALE&from=2026-09-01T00:00:00Z&to=2026-09-30T23:59:59Z&supplierId=20',
    );
    expect(request.request.method).toBe('GET');
    request.flush({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  });
});
