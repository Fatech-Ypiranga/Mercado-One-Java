import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CatalogService, ProductRequest } from './catalog.service';

describe('CatalogService', () => {
  let catalog: CatalogService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    catalog = TestBed.inject(CatalogService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('lists categories with trimmed filters', () => {
    catalog.listCategories({ search: ' Bebidas ', active: true }).subscribe();

    const request = http.expectOne('http://localhost:8080/api/catalog/categories?search=Bebidas&active=true');
    expect(request.request.method).toBe('GET');
    request.flush({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  });

  it('creates and updates categories', () => {
    catalog.createCategory({ name: 'Bebidas', active: true }).subscribe();
    let request = http.expectOne('http://localhost:8080/api/catalog/categories');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Bebidas', active: true });
    request.flush({ success: true, data: null, error: null, timestamp: new Date().toISOString() });

    catalog.updateCategory(2, { name: 'Mercearia', active: false }).subscribe();
    request = http.expectOne('http://localhost:8080/api/catalog/categories/2');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ name: 'Mercearia', active: false });
    request.flush({ success: true, data: null, error: null, timestamp: new Date().toISOString() });
  });

  it('lists products with category and status filters', () => {
    catalog.listProducts({ search: ' cafe ', categoryId: 3, active: false }).subscribe();

    const request = http.expectOne('http://localhost:8080/api/catalog/products?search=cafe&active=false&categoryId=3');
    expect(request.request.method).toBe('GET');
    request.flush({ success: true, data: [], error: null, timestamp: new Date().toISOString() });
  });

  it('creates and updates products with optional fiscal fields', () => {
    const body: ProductRequest = {
      name: 'Cafe',
      barcode: null,
      sku: 'CAF-001',
      categoryId: 1,
      unit: 'UN',
      salePrice: 12.9,
      active: true,
      ncm: '0901',
      cest: null,
      defaultCfop: null,
      merchandiseOrigin: null,
      taxClassification: null,
    };

    catalog.createProduct(body).subscribe();
    let request = http.expectOne('http://localhost:8080/api/catalog/products');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    request.flush({ success: true, data: null, error: null, timestamp: new Date().toISOString() });

    catalog.updateProduct(5, body).subscribe();
    request = http.expectOne('http://localhost:8080/api/catalog/products/5');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual(body);
    request.flush({ success: true, data: null, error: null, timestamp: new Date().toISOString() });
  });
});
