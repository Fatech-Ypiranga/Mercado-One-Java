import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { CatalogService } from '../core/catalog.service';
import { InventoryService } from '../core/inventory.service';
import { InventoryPageComponent } from './inventory-page.component';

describe('InventoryPageComponent', () => {
  const product = {
    id: 10,
    name: 'Cafe',
    barcode: null,
    sku: 'CAF-001',
    category: { id: 1, name: 'Bebidas', active: true },
    unit: 'UN',
    salePrice: 12.9,
    active: true,
    ncm: null,
    cest: null,
    defaultCfop: null,
    merchandiseOrigin: null,
    taxClassification: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InventoryPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
  });

  it('requires reason when registering an adjustment', () => {
    const catalog = TestBed.inject(CatalogService);
    const inventory = TestBed.inject(InventoryService);
    spyOn(catalog, 'listProducts').and.returnValue(of({ success: true, data: [product], error: null, timestamp: new Date().toISOString() }));
    spyOn(inventory, 'listBalances').and.returnValue(of({ success: true, data: [], error: null, timestamp: new Date().toISOString() }));
    spyOn(inventory, 'listMovements').and.returnValue(of({ success: true, data: [], error: null, timestamp: new Date().toISOString() }));
    const adjustment = spyOn(inventory, 'registerAdjustment').and.returnValue(of({ success: true, data: null as never, error: null, timestamp: new Date().toISOString() }));
    const fixture = TestBed.createComponent(InventoryPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].patchValue({
      type: 'ADJUSTMENT',
      productId: 10,
      newQuantity: 3,
      reason: '',
    });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(adjustment).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('justificativa');
  });

  it('registers an entry and reloads inventory data', () => {
    const catalog = TestBed.inject(CatalogService);
    const inventory = TestBed.inject(InventoryService);
    spyOn(catalog, 'listProducts').and.returnValue(of({ success: true, data: [product], error: null, timestamp: new Date().toISOString() }));
    spyOn(inventory, 'listBalances').and.returnValue(of({ success: true, data: [], error: null, timestamp: new Date().toISOString() }));
    spyOn(inventory, 'listMovements').and.returnValue(of({ success: true, data: [], error: null, timestamp: new Date().toISOString() }));
    const entry = spyOn(inventory, 'registerEntry').and.returnValue(of({ success: true, data: null as never, error: null, timestamp: new Date().toISOString() }));
    const fixture = TestBed.createComponent(InventoryPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].patchValue({
      type: 'ENTRY',
      productId: 10,
      quantity: 5,
      supplierName: 'Fornecedor',
    });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(entry).toHaveBeenCalledWith(jasmine.objectContaining({ productId: 10, quantity: 5 }));
    expect(fixture.nativeElement.textContent).toContain('Movimentacao registrada.');
  });

  it('shows feedback when movement registration fails', () => {
    const catalog = TestBed.inject(CatalogService);
    const inventory = TestBed.inject(InventoryService);
    spyOn(catalog, 'listProducts').and.returnValue(of({ success: true, data: [product], error: null, timestamp: new Date().toISOString() }));
    spyOn(inventory, 'listBalances').and.returnValue(of({ success: true, data: [], error: null, timestamp: new Date().toISOString() }));
    spyOn(inventory, 'listMovements').and.returnValue(of({ success: true, data: [], error: null, timestamp: new Date().toISOString() }));
    spyOn(inventory, 'registerEntry').and.returnValue(throwError(() => new HttpErrorResponse({ status: 400 })));
    const fixture = TestBed.createComponent(InventoryPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].patchValue({ type: 'ENTRY', productId: 10, quantity: 5 });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Nao foi possivel registrar a movimentacao.');
  });

  it('converts movement period filters from local day boundaries', () => {
    const catalog = TestBed.inject(CatalogService);
    const inventory = TestBed.inject(InventoryService);
    spyOn(catalog, 'listProducts').and.returnValue(of({ success: true, data: [product], error: null, timestamp: new Date().toISOString() }));
    spyOn(inventory, 'listBalances').and.returnValue(of({ success: true, data: [], error: null, timestamp: new Date().toISOString() }));
    const movements = spyOn(inventory, 'listMovements').and.returnValue(of({ success: true, data: [], error: null, timestamp: new Date().toISOString() }));
    const fixture = TestBed.createComponent(InventoryPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['movementFromControl'].setValue('2026-09-07');
    fixture.componentInstance['movementToControl'].setValue('2026-09-08');
    fixture.componentInstance['load']();

    expect(movements.calls.mostRecent().args[0]).toEqual(jasmine.objectContaining({
      from: new Date(2026, 8, 7, 0, 0, 0, 0).toISOString(),
      to: new Date(2026, 8, 8, 23, 59, 59, 999).toISOString(),
    }));
  });
});
