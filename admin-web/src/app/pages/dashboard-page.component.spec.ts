import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService, UserRole } from '../core/auth.service';
import { DashboardPageComponent } from './dashboard-page.component';

describe('DashboardPageComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPageComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  function setRole(role: UserRole): void {
    TestBed.inject(AuthService).session.set({
      expiresAt: new Date(Date.now() + 60_000).toISOString(),
      user: { id: 1, nome: 'Usuário', login: 'user', perfil: role },
    });
  }

  it('limits the stock clerk dashboard to permitted shortcuts and requests', () => {
    setRole('ESTOQUISTA');
    const fixture = TestBed.createComponent(DashboardPageComponent);
    TestBed.inject(HttpTestingController).expectOne((request) => request.url.endsWith('/api/system/info'))
      .flush({ success: true, data: { name: 'Mercado One', version: '1', roles: [] }, error: null, timestamp: '' });
    fixture.detectChanges();

    const links = Array.from(fixture.nativeElement.querySelectorAll('.department-index a')) as HTMLAnchorElement[];
    expect(links.map((link) => link.textContent?.trim())).toEqual([jasmine.stringMatching('Estoque')]);
    expect(fixture.nativeElement.textContent).toContain('restrito a administrador e gerente');
  });

  it('shows operational totals and pending conflicts for an admin', () => {
    setRole('ADMIN');
    const fixture = TestBed.createComponent(DashboardPageComponent);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne((request) => request.url.endsWith('/api/system/info'))
      .flush({ success: true, data: { name: 'Mercado One', version: '1', roles: [] }, error: null, timestamp: '' });
    http.expectOne((request) => request.url.endsWith('/api/offline/sales/conflicts'))
      .flush({ success: true, data: [{ id: 7, localSaleId: 'PDV-7', conflictSummary: 'Preço divergente' }], error: null, timestamp: '' });
    http.expectOne((request) => request.url.endsWith('/api/sales'))
      .flush({ success: true, data: { items: [], page: { page: 0, size: 1, totalElements: 0, totalPages: 0 }, totals: { saleCount: 2, totalAmount: 35, totalItems: 3, totalsByPaymentMethod: { PIX: 35 } } }, error: null, timestamp: '' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('PDV-7');
    expect(fixture.nativeElement.textContent).toContain('R$');
    expect(fixture.nativeElement.textContent).toContain('Pix');
  });

  it('keeps the newest dashboard refresh when older responses finish later', () => {
    setRole('ADMIN');
    const fixture = TestBed.createComponent(DashboardPageComponent);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne((request) => request.url.endsWith('/api/system/info'))
      .flush({ success: true, data: { name: 'Mercado One', version: '1', roles: [] }, error: null, timestamp: '' });

    fixture.componentInstance['refreshOperations']();
    const conflictRequests = http.match((request) => request.url.endsWith('/api/offline/sales/conflicts'));
    const salesRequests = http.match((request) => request.url.endsWith('/api/sales'));
    expect(conflictRequests.length).toBe(2);
    expect(salesRequests.length).toBe(2);

    conflictRequests[1].flush({ success: true, data: [{ id: 2, localSaleId: 'PDV-NOVO', conflictSummary: 'Novo' }], error: null, timestamp: '' });
    salesRequests[1].flush({ success: true, data: { items: [], page: { page: 0, size: 1, totalElements: 0, totalPages: 0 }, totals: { saleCount: 9, totalAmount: 90, totalItems: 9, totalsByPaymentMethod: { CASH: 90 } } }, error: null, timestamp: '' });
    conflictRequests[0].flush({ success: true, data: [{ id: 1, localSaleId: 'PDV-ANTIGO', conflictSummary: 'Antigo' }], error: null, timestamp: '' });
    salesRequests[0].flush({ success: true, data: { items: [], page: { page: 0, size: 1, totalElements: 0, totalPages: 0 }, totals: { saleCount: 1, totalAmount: 10, totalItems: 1, totalsByPaymentMethod: { PIX: 10 } } }, error: null, timestamp: '' });
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('PDV-NOVO');
    expect(fixture.nativeElement.textContent).toContain('Dinheiro');
    expect(fixture.nativeElement.textContent).not.toContain('PDV-ANTIGO');
    expect(fixture.nativeElement.textContent).not.toContain('Pix');
  });
});
