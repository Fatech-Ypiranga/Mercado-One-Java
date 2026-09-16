import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { CatalogService, Category, Product } from '../core/catalog.service';
import { ProductsPageComponent } from './products-page.component';

describe('ProductsPageComponent', () => {
  const now = new Date().toISOString();
  const category: Category = { id: 1, name: 'Bebidas', active: true, createdAt: now, updatedAt: now };
  const product: Product = {
    id: 10,
    name: 'Cafe',
    barcode: null,
    sku: 'CAF-001',
    category,
    unit: 'UN',
    salePrice: 12.9,
    active: true,
    ncm: null,
    cest: null,
    defaultCfop: null,
    merchandiseOrigin: null,
    taxClassification: null,
    createdAt: now,
    updatedAt: now,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
  });

  it('renders loaded products and categories', () => {
    const catalog = TestBed.inject(CatalogService);
    spyOn(catalog, 'listCategories').and.returnValue(of({ success: true, data: [category], error: null, timestamp: now }));
    spyOn(catalog, 'listProducts').and.returnValue(of({ success: true, data: [product], error: null, timestamp: now }));

    const fixture = TestBed.createComponent(ProductsPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Cafe');
    expect(fixture.nativeElement.textContent).toContain('CAF-001');
    expect(fixture.nativeElement.textContent).toContain('Bebidas');
  });

  it('normalizes blank optional fields when creating a product', () => {
    const catalog = TestBed.inject(CatalogService);
    spyOn(catalog, 'listCategories').and.returnValue(of({ success: true, data: [category], error: null, timestamp: now }));
    spyOn(catalog, 'listProducts').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    const create = spyOn(catalog, 'createProduct').and.returnValue(of({ success: true, data: product, error: null, timestamp: now }));
    const fixture = TestBed.createComponent(ProductsPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].patchValue({
      name: 'Cafe',
      barcode: '   ',
      sku: ' CAF-001 ',
      categoryId: 1,
      unit: 'UN',
      salePrice: 12.9,
      active: true,
    });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));

    expect(create).toHaveBeenCalledWith(jasmine.objectContaining({
      barcode: null,
      sku: 'CAF-001',
      categoryId: 1,
      salePrice: 12.9,
    }));
  });

  it('updates an existing product', () => {
    const catalog = TestBed.inject(CatalogService);
    spyOn(catalog, 'listCategories').and.returnValue(of({ success: true, data: [category], error: null, timestamp: now }));
    spyOn(catalog, 'listProducts').and.returnValue(of({ success: true, data: [product], error: null, timestamp: now }));
    const update = spyOn(catalog, 'updateProduct').and.returnValue(of({ success: true, data: product, error: null, timestamp: now }));
    const fixture = TestBed.createComponent(ProductsPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['edit'](product);
    fixture.componentInstance['form'].patchValue({ name: 'Cafe Especial' });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));

    expect(update).toHaveBeenCalledWith(10, jasmine.objectContaining({ name: 'Cafe Especial' }));
  });

  it('shows feedback when product save fails', () => {
    const catalog = TestBed.inject(CatalogService);
    spyOn(catalog, 'listCategories').and.returnValue(of({ success: true, data: [category], error: null, timestamp: now }));
    spyOn(catalog, 'listProducts').and.returnValue(of({ success: true, data: [], error: null, timestamp: now }));
    spyOn(catalog, 'createProduct').and.returnValue(throwError(() => new Error('fail')));
    const fixture = TestBed.createComponent(ProductsPageComponent);
    fixture.detectChanges();

    fixture.componentInstance['form'].patchValue({ name: 'Cafe', categoryId: 1, unit: 'UN', salePrice: 12.9 });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Revise os dados do produto.');
  });
});
