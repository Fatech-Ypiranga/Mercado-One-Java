import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { SupplierRequest, SupplierService } from './supplier.service';

describe('SupplierService', () => {
  let suppliers: SupplierService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    suppliers = TestBed.inject(SupplierService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('lists suppliers with trimmed filters', () => {
    suppliers.listSuppliers({ search: ' Acme ', active: false }).subscribe();

    const request = http.expectOne('http://localhost:8080/api/suppliers?search=Acme&active=false');
    expect(request.request.method).toBe('GET');
    request.flush({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  });

  it('creates and updates suppliers', () => {
    const body: SupplierRequest = {
      name: 'Acme Distribuidora',
      document: '123',
      phone: null,
      email: 'compras@example.com',
      notes: null,
      active: true,
    };

    suppliers.createSupplier(body).subscribe();
    let request = http.expectOne('http://localhost:8080/api/suppliers');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({ success: true, data: null, error: null, timestamp: new Date().toISOString() });

    suppliers.updateSupplier(7, body).subscribe();
    request = http.expectOne('http://localhost:8080/api/suppliers/7');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(body);
    request.flush({ success: true, data: null, error: null, timestamp: new Date().toISOString() });
  });
});
