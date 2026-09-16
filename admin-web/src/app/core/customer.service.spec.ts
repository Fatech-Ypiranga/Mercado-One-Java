import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { CustomerService } from './customer.service';

describe('CustomerService', () => {
  let customers: CustomerService;
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    customers = TestBed.inject(CustomerService);
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

  it('lists customers with filters and credentials', () => {
    customers.listCustomers({ search: 'maria', active: true }).subscribe();

    const request = http.expectOne('http://localhost:8080/api/customers?search=maria&active=true');
    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBeTrue();
    request.flush({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  });

  it('creates a customer', () => {
    customers.createCustomer({
      name: 'Maria Silva',
      phone: '11999990000',
      email: null,
      document: null,
      contactConsent: true,
      active: true,
    }).subscribe();

    const request = http.expectOne('http://localhost:8080/api/customers');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.name).toBe('Maria Silva');
    request.flush({ success: true, data: null, error: null, timestamp: new Date().toISOString() });
  });
});
