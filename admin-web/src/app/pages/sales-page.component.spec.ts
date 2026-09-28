import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { ApiEnvelope } from '../core/api-client.service';
import { Sale, SalesReport, SalesService } from '../core/sales.service';
import { SalesPageComponent } from './sales-page.component';

describe('SalesPageComponent', () => {
  const now = new Date().toISOString();
  function envelope(data: SalesReport): ApiEnvelope<SalesReport> {
    return { success: true, data, error: null, timestamp: now };
  }
  function report(totalElements: number): SalesReport {
    return {
      items: [],
      page: { page: 0, size: 20, totalElements, totalPages: 1 },
      totals: { saleCount: totalElements, totalAmount: 0, totalItems: 0, totalsByPaymentMethod: {} },
    };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SalesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({ customerId: '42' }) } } },
      ],
    }).compileComponents();
  });

  it('uses customer history query and resets pagination when filters are applied', () => {
    const sales = TestBed.inject(SalesService);
    const list = spyOn(sales, 'listSales').and.returnValue(of(envelope(report(2))));
    spyOn(sales, 'topProducts').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    const fixture = TestBed.createComponent(SalesPageComponent);
    fixture.detectChanges();
    expect(list.calls.mostRecent().args[0]).toEqual(jasmine.objectContaining({ customerId: 42, page: 0 }));
    fixture.componentInstance['currentPage'] = 3;
    fixture.componentInstance['applyFilters']();
    expect(list.calls.mostRecent().args[0]).toEqual(jasmine.objectContaining({ customerId: 42, page: 0 }));
  });

  it('keeps the newest report when an older filter request finishes later', () => {
    const sales = TestBed.inject(SalesService);
    const first = new Subject<ApiEnvelope<SalesReport>>();
    const second = new Subject<ApiEnvelope<SalesReport>>();
    spyOn(sales, 'listSales').and.returnValues(first.asObservable(), second.asObservable());
    spyOn(sales, 'topProducts').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    const fixture = TestBed.createComponent(SalesPageComponent);
    fixture.detectChanges();
    fixture.componentInstance['applyFilters']();
    second.next(envelope(report(7)));
    second.complete();
    first.next(envelope(report(99)));
    first.complete();
    fixture.detectChanges();
    expect(fixture.componentInstance['report']?.page.totalElements).toBe(7);
    expect(fixture.componentInstance['loading']).toBeFalse();
  });

  it('keeps the last report visible when refreshing sales fails', () => {
    const sales = TestBed.inject(SalesService);
    spyOn(sales, 'listSales').and.returnValues(
      of(envelope(report(2))),
      throwError(() => new HttpErrorResponse({ status: 503 })),
    );
    spyOn(sales, 'topProducts').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    const fixture = TestBed.createComponent(SalesPageComponent);
    fixture.detectChanges();
    fixture.componentInstance['applyFilters']();
    fixture.detectChanges();

    expect(fixture.componentInstance['report']?.page.totalElements).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Não foi possível carregar vendas.');
    expect(fixture.componentInstance['loading']).toBeFalse();
  });

  it('blocks repeated CSV requests and recovers after export failure', () => {
    const sales = TestBed.inject(SalesService);
    spyOn(sales, 'listSales').and.returnValue(of(envelope(report(1))));
    spyOn(sales, 'topProducts').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    const pending = new Subject<string>();
    const exportCsv = spyOn(sales, 'exportCsv').and.returnValue(pending.asObservable());
    const fixture = TestBed.createComponent(SalesPageComponent);
    fixture.detectChanges();
    fixture.componentInstance['exportCsv']();
    fixture.componentInstance['exportCsv']();
    expect(exportCsv).toHaveBeenCalledTimes(1);
    pending.error(new HttpErrorResponse({ status: 503 }));
    fixture.detectChanges();

    expect(fixture.componentInstance['exporting']).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Não foi possível exportar vendas.');
  });

  it('formats payment amounts in Brazilian currency', () => {
    const fixture = TestBed.createComponent(SalesPageComponent);
    expect(fixture.componentInstance['paymentSummary']({
      payments: [{ method: 'CASH', amount: 16.5 }],
    } as Sale).replace(/\s/g, ' ')).toBe('Dinheiro R$ 16,50');
  });
});
