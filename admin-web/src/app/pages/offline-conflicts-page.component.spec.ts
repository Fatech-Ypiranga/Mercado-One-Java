import { provideHttpClient } from '@angular/common/http';
import { HttpErrorResponse } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';

import { ApiEnvelope } from '../core/api-client.service';
import { OfflineConflict, OfflineService } from '../core/offline.service';
import { OfflineConflictsPageComponent } from './offline-conflicts-page.component';

describe('OfflineConflictsPageComponent', () => {
  const conflictPayload = JSON.stringify({
    localSaleId: 'local-1',
    createdAt: new Date().toISOString(),
    customerId: null,
    items: [{ productId: 1, quantity: 2, unitPrice: 7.5 }],
    payments: [{ method: 'CASH', amount: 15 }],
  });
  const now = new Date().toISOString();

  function conflict(overrides: Partial<OfflineConflict> = {}): OfflineConflict {
    return {
      id: 10,
      localSaleId: 'local-1',
      createdAt: now,
      customerId: null,
      operatorUserId: 2,
      status: 'PENDING',
      conflictSummary: 'PRICE_CHANGED: Preco mudou.',
      salePayload: conflictPayload,
      remoteSaleId: null,
      resolutionNote: null,
      resolvedByUserId: null,
      resolvedAt: null,
      updatedAt: now,
      ...overrides,
    };
  }

  function envelope(data: OfflineConflict[]): ApiEnvelope<OfflineConflict[]> {
    return { success: true, data, error: null, timestamp: now };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OfflineConflictsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
  });

  it('renders pending conflicts and resolves them', () => {
    const offline = TestBed.inject(OfflineService);
    spyOn(offline, 'listConflicts').and.returnValues(of(envelope([conflict()])), of(envelope([])));
    const resolve = spyOn(offline, 'resolveConflict').and.returnValue(of({
      success: true,
      data: conflict({ status: 'ACCEPTED', remoteSaleId: 20 }),
      error: null,
      timestamp: now,
    }));

    const fixture = TestBed.createComponent(OfflineConflictsPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('local-1');
    expect(fixture.nativeElement.textContent).toContain('Total offline');
    expect(fixture.nativeElement.textContent).toContain('Produto #1');
    expect(fixture.nativeElement.textContent).toContain('Dinheiro');
    fixture.nativeElement.querySelector('.primary-button').click();
    fixture.detectChanges();

    expect(resolve).toHaveBeenCalledWith(10, { action: 'ACCEPT', note: null });
    expect(fixture.nativeElement.textContent).toContain('Conflito aceito e venda registrada.');
  });

  it('requires a note before rejecting a conflict', () => {
    const offline = TestBed.inject(OfflineService);
    spyOn(offline, 'listConflicts').and.returnValue(of(envelope([conflict()])));
    const resolve = spyOn(offline, 'resolveConflict').and.returnValue(of({
      success: true,
      data: null as never,
      error: null,
      timestamp: now,
    }));

    const fixture = TestBed.createComponent(OfflineConflictsPageComponent);
    fixture.detectChanges();

    fixture.nativeElement.querySelector('.button-row .ghost-button').click();
    fixture.detectChanges();

    expect(resolve).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Informe uma observação para rejeitar o conflito.');
  });

  it('renders the loading state', () => {
    const offline = TestBed.inject(OfflineService);
    const response = new Subject<ApiEnvelope<OfflineConflict[]>>();
    spyOn(offline, 'listConflicts').and.returnValue(response.asObservable());

    const fixture = TestBed.createComponent(OfflineConflictsPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Carregando conflitos...');
  });

  it('renders the empty state', () => {
    const offline = TestBed.inject(OfflineService);
    spyOn(offline, 'listConflicts').and.returnValue(of(envelope([])));

    const fixture = TestBed.createComponent(OfflineConflictsPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nenhum conflito encontrado');
  });

  it('renders API errors when conflict loading fails', () => {
    const offline = TestBed.inject(OfflineService);
    spyOn(offline, 'listConflicts').and.returnValue(throwError(() => new HttpErrorResponse({
      status: 500,
      error: {
        success: false,
        data: null,
        error: { code: 'INTERNAL_ERROR', message: 'Falha ao buscar conflitos.', details: [] },
        timestamp: now,
      },
    })));

    const fixture = TestBed.createComponent(OfflineConflictsPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Falha ao buscar conflitos.');
  });

  it('shows resolved conflict notes instead of action buttons', () => {
    const offline = TestBed.inject(OfflineService);
    spyOn(offline, 'listConflicts').and.returnValue(of(envelope([conflict({
      status: 'REJECTED',
      resolutionNote: 'Venda duplicada no PDV.',
    })])));

    const fixture = TestBed.createComponent(OfflineConflictsPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Venda duplicada no PDV.');
    expect(fixture.nativeElement.querySelector('.button-row')).toBeNull();
  });
});
