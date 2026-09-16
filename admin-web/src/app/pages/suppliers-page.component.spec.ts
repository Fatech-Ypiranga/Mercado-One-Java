import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { Supplier, SupplierService } from '../core/supplier.service';
import { SuppliersPageComponent } from './suppliers-page.component';

describe('SuppliersPageComponent', () => {
  const now = new Date().toISOString();
  const supplier: Supplier = {
    id: 8,
    name: 'Acme Distribuidora',
    document: '123',
    phone: '1133334444',
    email: 'compras@example.com',
    notes: null,
    active: true,
    createdAt: now,
    updatedAt: now,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SuppliersPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
  });

  it('renders loaded suppliers', () => {
    const suppliers = TestBed.inject(SupplierService);
    spyOn(suppliers, 'listSuppliers').and.returnValue(of({ success: true, data: [supplier], error: null, timestamp: now }));

    const fixture = TestBed.createComponent(SuppliersPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Acme Distribuidora');
    expect(fixture.nativeElement.textContent).toContain('compras@example.com');
  });

  it('normalizes optional fields when creating suppliers', () => {
    const suppliers = TestBed.inject(SupplierService);
    spyOn(suppliers, 'listSuppliers').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    const create = spyOn(suppliers, 'createSupplier').and.returnValue(of({ success: true, data: supplier, error: null, timestamp: now }));
    const fixture = TestBed.createComponent(SuppliersPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].setValue({
      name: 'Acme Distribuidora',
      document: ' 123 ',
      phone: '',
      email: 'compras@example.com',
      notes: '   ',
      active: true,
    });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));

    expect(create).toHaveBeenCalledWith({
      name: 'Acme Distribuidora',
      document: '123',
      phone: null,
      email: 'compras@example.com',
      notes: null,
      active: true,
    });
  });

  it('updates suppliers and shows save feedback', () => {
    const suppliers = TestBed.inject(SupplierService);
    spyOn(suppliers, 'listSuppliers').and.returnValue(of({ success: true, data: [supplier], error: null, timestamp: now }));
    const update = spyOn(suppliers, 'updateSupplier').and.returnValue(of({ success: true, data: supplier, error: null, timestamp: now }));
    const fixture = TestBed.createComponent(SuppliersPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['edit'](supplier);
    fixture.componentInstance['form'].patchValue({ name: 'Acme Atacado' });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(update).toHaveBeenCalledWith(8, jasmine.objectContaining({ name: 'Acme Atacado' }));
    expect(fixture.nativeElement.textContent).toContain('Fornecedor salvo.');
  });

  it('shows feedback when saving fails', () => {
    const suppliers = TestBed.inject(SupplierService);
    spyOn(suppliers, 'listSuppliers').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    spyOn(suppliers, 'createSupplier').and.returnValue(throwError(() => new Error('fail')));
    const fixture = TestBed.createComponent(SuppliersPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].patchValue({ name: 'Acme Distribuidora' });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Revise os dados do fornecedor.');
  });
});
